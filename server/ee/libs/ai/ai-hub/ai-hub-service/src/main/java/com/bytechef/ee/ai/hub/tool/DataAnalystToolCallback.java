/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the {@code data_analyst} subagent to the parent ai_hub BUILD
 * agent.
 *
 * <p>
 * When the parent LLM invokes this tool it passes a JSON object with a {@code question} field (and an optional
 * {@code dataTableId}). The callback delegates to a pre-configured {@link ChatClient} that carries data-table tools
 * ({@code listDataTables}, {@code queryDataTable}, {@code aggregateDataTable}, {@code openDataTableTab}) and the
 * {@code prompt_data_analyst.txt} system prompt. The isolated chat client context means the parent never sees the
 * analysis transcript; it only receives the synthesised markdown report returned by {@code call()}.
 *
 * <p>
 * If the report exceeds 2000 characters it is persisted via {@link AssetFileFacade#createFromAi} and a compact summary
 * payload is returned to the parent instead, keeping the parent's context lean.
 *
 * <p>
 * <b>Why hand-rolled instead of {@code TaskTool.builder()}?</b> The same reason as
 * {@link com.bytechef.ee.ai.hub.tool.ResearchToolCallback} — no API exists to register a named subagent backed by an
 * externally-constructed {@link ChatClient}. The hand-rolled approach achieves the same architecture without
 * incompatible tooling.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DataAnalystToolCallback implements ToolCallback {

    static final int INLINE_REPORT_MAX_LENGTH = 2000;
    static final int SUMMARY_PREVIEW_LENGTH = 300;

    private static final Logger log = LoggerFactory.getLogger(DataAnalystToolCallback.class);

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final String DESCRIPTION =
        """
            Delegate an analytical question about workspace data tables to a specialised
            data_analyst subagent. The subagent discovers available data tables, queries and
            aggregates them (sum/avg/count/min/max, optional group-by), and returns a concise
            markdown analysis report with findings, method, and caveats.

            Use this tool when the user asks about totals, averages, distributions, groupings,
            or any question that requires reading and analysing data table contents. Pass the
            user's question verbatim as the 'question' field. Optionally supply 'dataTableId'
            when the user has already identified a specific table.

            After the report returns, save it via createAssetFile and open it with
            openResourceTab (type FILE); summarise findings in one paragraph in chat.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "question": {
                        "type": "string",
                        "description": "The analytical question to answer about the workspace data tables. Be specific about what totals, groupings, or distributions are needed."
                    },
                    "dataTableId": {
                        "type": "string",
                        "description": "Optional data table id to focus analysis on. Obtained from listDataTables. If omitted the subagent will discover tables itself."
                    }
                },
                "required": ["question"]
            }""";

    private final AssetFileFacade assetFileFacade;
    private final ChatClient dataAnalystChatClient;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataAnalystToolCallback(ChatClient dataAnalystChatClient, AssetFileFacade assetFileFacade) {
        this.assetFileFacade = assetFileFacade;
        this.dataAnalystChatClient = dataAnalystChatClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("data_analyst")
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
            DataAnalystInput input = jsonMapper.readValue(toolInput, DataAnalystInput.class);

            String question = input.question();

            if (question == null || question.isBlank()) {
                return toolError("question is required and must not be blank");
            }

            String dataTableId = input.dataTableId();

            String prompt = dataTableId != null && !dataTableId.isBlank()
                ? question + "\n\nFocus on data table id: " + dataTableId
                : question;

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            String finalPrompt = prompt;

            String result = CurrentAgentContext.callWith(AiHubAgentType.DATA_ANALYST, parentAgent,
                () -> dataAnalystChatClient.prompt(finalPrompt)
                    .call()
                    .content());

            if (result == null) {
                log.warn(
                    "data_analyst subagent returned null for question='{}'",
                    input.question());

                return ToolErrors.toolError(jsonMapper, "data_analyst subagent returned null");
            }

            if (result.length() > INLINE_REPORT_MAX_LENGTH) {
                return spillReportToFile(result, toolContext);
            }

            return result;
        } catch (JacksonException exception) {
            // Malformed tool input is a recoverable LLM error — return a typed tool error so the agent can retry.
            // Log at WARN so a sudden spike (bad system prompt, schema drift) is visible in production logs without
            // requiring user reports.
            log.warn(
                "data_analyst rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, DataAnalystToolCallback.class, "data_analyst", exception);
        }
    }

    private String spillReportToFile(String report, @Nullable ToolContext toolContext) throws JacksonException {
        String timestamp = LocalDateTime.now()
            .format(FILENAME_FORMATTER);
        String filename = "data-analyst-report-" + timestamp + ".md";

        AiHubToolInvocationContext invocationContext =
            AiHubToolInvocationContext.fromToolContext(toolContext);

        Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();
        Short sourceOrdinal = invocationContext == null ? null : invocationContext.sourceOrdinal();
        String lastUserPrompt = invocationContext == null ? null : invocationContext.lastUserPrompt();

        AssetFile assetFile = assetFileFacade.createFromAi(
            workspaceId, AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
            filename, "text/markdown", report, null, null, sourceOrdinal, lastUserPrompt);

        String preview = report.length() > SUMMARY_PREVIEW_LENGTH
            ? report.substring(0, SUMMARY_PREVIEW_LENGTH) + "..."
            : report;

        return jsonMapper.writeValueAsString(Map.of(
            "reportFileId", assetFile.getId(),
            "reportName", filename,
            "summary", preview,
            "truncatedIntoFile", true));
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DataAnalystInput(String question, @Nullable String dataTableId) {
    }
}
