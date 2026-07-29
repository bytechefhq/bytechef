/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport;
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.DataTableNotFoundException;
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.WhereParseException;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.ee.ai.hub.artifact.ArtifactGeneratorRegistry;
import com.bytechef.ee.ai.hub.artifact.GenerationRequest;
import com.bytechef.ee.ai.hub.artifact.GenerationResult;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that queries rows from a data table by id. An optional simple equals filter can be
 * supplied via the {@code where} parameter (e.g. {@code "status = 'qualified'"}). Results are capped at 50 rows.
 *
 * <p>
 * This is the AI-Hub <em>superset</em> variant: on top of the inline-query behaviour it adds an {@code exportToCsv}
 * branch that materialises the rowset as a CSV {@code asset_file} through the EE AI-Hub artifact pipeline
 * ({@code ArtifactGeneratorRegistry} / {@code CsvArtifactGenerator} / {@code AiHubTaskService}). That branch is
 * genuinely AI-Hub-coupled, so it cannot live in the shared lib. The inline query/filter semantics are shared with the
 * canonical inline-only version {@link com.bytechef.automation.ai.tool.datatable.QueryDataTableToolCallback} through
 * {@link DataTableQuerySupport}; this class is retained only for the {@code data_analyst} subagent, which has the
 * artifact pipeline and needs CSV export.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class QueryDataTableToolCallback implements ToolCallback {

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;

    private static final String DESCRIPTION = """
        Query rows from a data table by its id. Optionally filter with a simple equals expression
        via the 'where' parameter (e.g. "status = 'qualified'" or "age = '30'"). Results are
        limited to 50 rows maximum by default. Returns a JSON array where each element is a row
        object with column names as keys. Obtain the dataTableId from listDataTables first.

        When `exportToCsv` is true, the result rows are written as a CSV asset_file (format=CSV,
        source=AI_GENERATED) registered with the task, and the response carries
        {exported, assetFileId, filename, rowCount} instead of the inline rows. Use this when
        the user asks "give me a CSV of...", "export... to file", or when the result set is
        large enough that an inline JSON array would clutter chat. The csv export bypasses the
        50-row inline cap and is bounded by the data table's natural size.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "dataTableId": {"type": "string", "description": "Data table id obtained from listDataTables"},
                    "where": {"type": "string", "description": "Optional simple equals filter, e.g. \\"status = 'qualified'\\""},
                    "limit": {"type": "integer", "description": "Maximum number of rows to return (capped at 50 for inline; ignored when exportToCsv=true)"},
                    "exportToCsv": {"type": "boolean", "description": "When true, write the result rows as a CSV asset_file registered with the task rather than returning them inline"}
                },
                "required": ["dataTableId"]
            }""";

    private static final int CSV_EXPORT_MAX_ROWS = 10_000;

    @Nullable
    private final ArtifactGeneratorRegistry artifactGeneratorRegistry;
    @Nullable
    private final AiHubTaskService taskService;
    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public QueryDataTableToolCallback(
        ArtifactGeneratorRegistry artifactGeneratorRegistry,
        AiHubTaskService taskService,
        DataTableRowService dataTableRowService,
        DataTableService dataTableService) {

        this.artifactGeneratorRegistry = artifactGeneratorRegistry;
        this.taskService = taskService;
        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
    }

    /**
     * Convenience constructor for sub-agents that don't need the CSV-export branch (e.g. the data analyst, which has
     * its own asset_file write path via {@code DataAnalystToolCallback.spillReportToFile}). Calls with
     * {@code exportToCsv=true} are rejected at the tool boundary with a structured error pointing the LLM at the inline
     * path.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public QueryDataTableToolCallback(
        DataTableRowService dataTableRowService, DataTableService dataTableService) {

        this(null, null, dataTableRowService, dataTableService);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("queryDataTable")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            QueryDataTableInput input = jsonMapper.readValue(toolInput, QueryDataTableInput.class);

            if (input.dataTableId() == null || input.dataTableId()
                .isBlank()) {
                return toolError("dataTableId is required");
            }

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext.workspaceId() == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            long dataTableId;

            try {
                dataTableId = Long.parseLong(input.dataTableId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid dataTableId - must be a numeric id obtained from listDataTables");
            }

            String baseName;

            try {
                baseName = DataTableQuerySupport.resolveBaseName(dataTableService, dataTableId);
            } catch (DataTableNotFoundException exception) {
                return toolError("Data table not found: " + input.dataTableId());
            }

            boolean exportToCsv = Boolean.TRUE.equals(input.exportToCsv());

            if (exportToCsv && (artifactGeneratorRegistry == null || taskService == null)) {
                return toolError(
                    "exportToCsv is not available in this tool context — request the inline rows instead.");
            }

            int fetchLimit = exportToCsv ? CSV_EXPORT_MAX_ROWS : DataTableQuerySupport.resolveLimit(input.limit());

            List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
                dataTableRowService, baseName, input.where(), fetchLimit, resolveEnvironmentId(invocationContext));

            if (exportToCsv) {
                return exportToCsvAsset(baseName, rowMaps, invocationContext);
            }

            return jsonMapper.writeValueAsString(rowMaps);
        } catch (WhereParseException exception) {
            return toolError("Invalid where clause: " + exception.getMessage()
                + ". Use simple equals syntax, e.g. \"status = 'qualified'\"");
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, QueryDataTableToolCallback.class, "queryDataTable", exception);
        }
    }

    private long resolveEnvironmentId(AiHubToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        return environmentId != null ? environmentId : DEFAULT_ENVIRONMENT_ORDINAL;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    /**
     * Materialises the rowset as a CSV {@code asset_file} via the existing CHART/JSON/MARKDOWN generator pipeline.
     * Reuses {@link com.bytechef.ee.ai.hub.artifact.CsvArtifactGenerator} so the generated file: (a) is registered to
     * the task via the {@code AUTHORED} join, (b) appears in the Generated tab on the Files page, and (c) inherits the
     * same workspace/env scoping as user-uploaded files. Returns a tool result envelope keyed by {@code exported:true}
     * so the LLM dispatches differently than the inline-JSON path.
     */
    private String exportToCsvAsset(
        String baseName, List<Map<String, Object>> rowMaps, AiHubToolInvocationContext invocationContext)
        throws JacksonException {

        String csv = renderCsv(rowMaps);
        String filename = baseName + "-export-"
            + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            + ".csv";

        Long taskId = resolveTaskId(invocationContext.threadId());

        GenerationRequest request = new GenerationRequest(
            invocationContext.workspaceId(),
            invocationContext.userId() == null ? 0L : invocationContext.userId(),
            (int) resolveEnvironmentId(invocationContext),
            taskId,
            invocationContext.sourceOrdinal() == null ? (short) 0 : invocationContext.sourceOrdinal(),
            invocationContext.lastUserPrompt(),
            filename,
            csv,
            null);

        GenerationResult result = artifactGeneratorRegistry.generate(AssetFileFormat.CSV, request);

        return jsonMapper.writeValueAsString(Map.of(
            "exported", true,
            "assetFileId", result.assetFileId(),
            "filename", result.filename(),
            "rowCount", rowMaps.size(),
            "taskLinked", result.taskLinked()));
    }

    /**
     * Resolves the task row for the bound thread so the CSV export can be linked. {@code null} on first-turn races or
     * when the tool is invoked outside a chat (rare for this tool); the artifact still persists, just without the
     * AUTHORED join.
     */
    private @Nullable Long resolveTaskId(@Nullable String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return null;
        }

        return taskService.findByThreadId(threadId)
            .map(AiHubTask::getId)
            .orElse(null);
    }

    /**
     * Renders the rowset into RFC 4180 CSV. Header row is the union of keys present across all rows (preserves
     * first-seen order) — using a union rather than the first row's keys handles the rare case where the data table has
     * a sparse schema and the first-returned rows happen to omit a column. Cell values that contain commas, quotes, or
     * newlines are double-quoted with internal {@code "} escaped as {@code ""}.
     */
    private static String renderCsv(List<Map<String, Object>> rowMaps) {
        Set<String> headers = new LinkedHashSet<>();

        for (Map<String, Object> row : rowMaps) {
            headers.addAll(row.keySet());
        }

        StringBuilder builder = new StringBuilder();
        boolean firstHeader = true;

        for (String header : headers) {
            if (!firstHeader) {
                builder.append(',');
            }

            builder.append(escapeCsvField(header));
            firstHeader = false;
        }

        builder.append('\n');

        for (Map<String, Object> row : rowMaps) {
            boolean firstCell = true;

            for (String header : headers) {
                if (!firstCell) {
                    builder.append(',');
                }

                Object value = row.get(header);

                builder.append(escapeCsvField(value == null ? "" : value.toString()));
                firstCell = false;
            }

            builder.append('\n');
        }

        return builder.toString();
    }

    private static String escapeCsvField(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }

        return '"' + value.replace("\"", "\"\"") + '"';
    }

    public record QueryDataTableInput(
        String dataTableId, @Nullable String where, @Nullable Integer limit, @Nullable Boolean exportToCsv) {
    }
}
