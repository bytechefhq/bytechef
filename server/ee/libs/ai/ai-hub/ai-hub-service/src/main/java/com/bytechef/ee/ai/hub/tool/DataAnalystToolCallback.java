/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DataAnalystToolCallback implements ToolCallback {

    static final String TOOL_NAME = "data_analyst";

    static final int INLINE_REPORT_MAX_LENGTH = 2000;
    static final int SUMMARY_PREVIEW_LENGTH = 300;

    private static final Logger log = LoggerFactory.getLogger(DataAnalystToolCallback.class);

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final String DESCRIPTION =
        """
            Delegate an analytical question about workspace data tables to a specialised
            data_analyst subagent. The subagent discovers the data tables, queries and
            aggregates them (sum/avg/count/min/max, optional group-by), and returns a concise
            markdown analysis report with findings, method, and caveats.

            Use this tool when the user asks about totals, averages, distributions, groupings,
            or any question that requires reading and analysing data table contents. Pass the
            user's question verbatim as the 'question' field. Optionally supply 'dataTableId'
            when the user has already identified a specific table.

            A short report comes back inline as 'report'. A long report is saved as a workspace
            file instead, and the result carries "truncatedIntoFile": true with reportFileId,
            reportName and a short summary; that file must NOT be saved again.""";

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

    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final AssetFileFacade assetFileFacade;
    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final List<ToolCallback> dataTableToolCallbacks;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataAnalystToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        List<ToolCallback> dataTableToolCallbacks, AssetFileFacade assetFileFacade,
        @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.artifactRecorder = artifactRecorder;
        this.assetFileFacade = assetFileFacade;
        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
        this.dataTableToolCallbacks = List.copyOf(dataTableToolCallbacks);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            DataAnalystInput input = JsonUtils.read(toolInput, DataAnalystInput.class);

            String question = input.question();

            if (question == null || question.isBlank()) {
                return ToolErrors.toolError("question is required and must not be blank");
            }

            String dataTableId = input.dataTableId();

            String prompt = dataTableId != null && !dataTableId.isBlank()
                ? question + "\n\nFocus on data table id: " + dataTableId
                : question;

            String report = SubAgentCalls.call(
                chatClientFactory, chatModelResolver, AiHubAgentType.DATA_ANALYST, prompt, dataTableToolCallbacks,
                SubAgentCalls.parentContext(toolContext));

            if (report == null) {
                log.warn("data_analyst subagent returned null for question='{}'", question);

                return ToolErrors.toolError("data_analyst subagent returned null");
            }

            if (report.length() > INLINE_REPORT_MAX_LENGTH) {
                String spilledReport = spillReportToFile(report, toolContext);

                if (spilledReport != null) {
                    return spilledReport;
                }
            }

            return JsonUtils.write(Map.of("report", report));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (ChatModelUnavailableException exception) {
            log.warn("data_analyst cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(DataAnalystToolCallback.class, TOOL_NAME, exception);
        }
    }

    private @Nullable String spillReportToFile(String report, @Nullable ToolContext toolContext) {
        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.workspaceId() == null) {
            return null;
        }

        String timestamp = LocalDateTime.now()
            .format(FILENAME_FORMATTER);

        AssetFile assetFile = assetFileFacade.createFromAi(
            invocationContext.workspaceId(), AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
            "data-analyst-report-" + timestamp + ".md", "text/markdown", report, null, null,
            invocationContext.sourceOrdinal(), invocationContext.lastUserPrompt());

        String reportFileId = String.valueOf(assetFile.getId());

        if (artifactRecorder != null && invocationContext.threadId() != null) {
            artifactRecorder.record(
                invocationContext.threadId(), invocationContext.userId(), "FILE_CREATED", reportFileId,
                assetFile.getName());
        }

        String summary = report.length() > SUMMARY_PREVIEW_LENGTH
            ? report.substring(0, SUMMARY_PREVIEW_LENGTH) + "..."
            : report;

        return JsonUtils.write(
            Map.of(
                "reportFileId", reportFileId, "reportName", assetFile.getName(), "summary", summary,
                "truncatedIntoFile", true));
    }

    public record DataAnalystInput(@Nullable String question, @Nullable String dataTableId) {
    }
}
