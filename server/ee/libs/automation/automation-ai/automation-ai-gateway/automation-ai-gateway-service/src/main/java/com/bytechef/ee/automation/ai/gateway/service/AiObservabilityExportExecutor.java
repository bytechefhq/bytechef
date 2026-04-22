/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportFormat;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.scheduler.event.ExportExecutionEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronously executes export jobs by querying the appropriate data source, formatting the results, and writing to
 * file storage.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiObservabilityExportExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AiObservabilityExportExecutor.class);

    private static final String EXPORT_DIRECTORY = "ai-gateway-exports";
    private static final int PROMPT_CONTENT_PREVIEW_LENGTH = 120;

    private final AiGatewayMetrics aiGatewayMetrics;
    private final AiObservabilityExportJobService aiObservabilityExportJobService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final AiGatewayRequestLogService aiGatewayRequestLogService;
    private final AiPromptService aiPromptService;
    private final AiPromptVersionService aiPromptVersionService;
    private final FileStorageService fileStorageService;
    private final ObjectProvider<AiObservabilityExportExecutor> selfProvider;

    public AiObservabilityExportExecutor(
        AiGatewayMetrics aiGatewayMetrics,
        AiObservabilityExportJobService aiObservabilityExportJobService,
        AiObservabilityTraceService aiObservabilityTraceService,
        AiObservabilitySessionService aiObservabilitySessionService,
        AiGatewayRequestLogService aiGatewayRequestLogService,
        AiPromptService aiPromptService,
        AiPromptVersionService aiPromptVersionService,
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry,
        ObjectProvider<AiObservabilityExportExecutor> selfProvider) {

        this.aiGatewayMetrics = aiGatewayMetrics;
        this.aiObservabilityExportJobService = aiObservabilityExportJobService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.aiGatewayRequestLogService = aiGatewayRequestLogService;
        this.aiPromptService = aiPromptService;
        this.aiPromptVersionService = aiPromptVersionService;
        this.selfProvider = selfProvider;

        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        this.fileStorageService = fileStorageServiceRegistry.getFileStorageService(provider.name());
    }

    /**
     * Listens for scheduler-fired cron events. Re-runs the export job identified by {@code exportJobId} by resetting
     * status → PENDING and dispatching {@link #executeExport(long)}. Terminal cancellation stops future re-runs via
     * {@code ExportScheduler.cancelExport}.
     */
    @EventListener
    public void onScheduledExport(ExportExecutionEvent event) {
        long exportJobId = event.exportJobId();

        try {
            AiObservabilityExportJob exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

            if (exportJob.getStatus() == AiObservabilityExportJobStatus.CANCELLED) {
                logger.info("Scheduled export {} is CANCELLED; not re-running", exportJobId);

                return;
            }

            if (exportJob.getStatus() == AiObservabilityExportJobStatus.PROCESSING) {
                logger.warn(
                    "Scheduled export {} is still PROCESSING from prior fire; skipping this tick", exportJobId);

                return;
            }

            // Reset to PENDING so the idempotent executeExport transitions it through PROCESSING again. Preserves the
            // single-row-per-schedule model rather than writing a new job row per fire; errorMessage/filePath are
            // overwritten on each successful cycle.
            exportJob.setStatus(AiObservabilityExportJobStatus.PENDING);

            aiObservabilityExportJobService.update(exportJob);

            // Resolve through the Spring proxy so @Async executes on the configured async executor rather than
            // synchronously on the Quartz worker thread.
            selfProvider.getObject()
                .executeExport(exportJobId);
        } catch (Exception exception) {
            logger.error(
                "Scheduled export dispatch failed for job {} — next cron tick will retry", exportJobId, exception);
        }
    }

    @Async
    public void executeExport(long exportJobId) {
        AiObservabilityExportJob exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

        if (exportJob.getStatus() == AiObservabilityExportJobStatus.CANCELLED) {
            logger.info("Export job {} already cancelled before start; skipping", exportJobId);

            return;
        }

        String scope = exportJob.getScope()
            .name();
        long startMs = System.currentTimeMillis();

        exportJob.setStatus(AiObservabilityExportJobStatus.PROCESSING);

        aiObservabilityExportJobService.update(exportJob);

        try {
            List<Map<String, Object>> records = fetchRecords(exportJob);

            // Re-check after fetch (can be long-running); if operator cancelled while we were fetching, abort before
            // paying the file-storage write cost.
            AiObservabilityExportJob refreshed = aiObservabilityExportJobService.getExportJob(exportJobId);

            if (refreshed.getStatus() == AiObservabilityExportJobStatus.CANCELLED) {
                logger.info("Export job {} cancelled after fetch; skipping file write", exportJobId);

                return;
            }

            String content = formatRecords(records, exportJob.getFormat());
            String filename = generateFilename(exportJob);

            FileEntry fileEntry = fileStorageService.storeFileContent(EXPORT_DIRECTORY, filename, content);

            exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

            if (exportJob.getStatus() == AiObservabilityExportJobStatus.CANCELLED) {
                logger.info(
                    "Export job {} cancelled after file write; leaving {} in file storage as partial artifact",
                    exportJobId, fileEntry.getUrl());

                return;
            }

            exportJob.setStatus(AiObservabilityExportJobStatus.COMPLETED);
            exportJob.setFilePath(fileEntry.getUrl());
            exportJob.setRecordCount(records.size());

            aiObservabilityExportJobService.update(exportJob);

            aiGatewayMetrics.recordExportDuration(scope, "success", System.currentTimeMillis() - startMs);
        } catch (Exception exception) {
            logger.error("Export job {} failed: {}", exportJobId, exception.getMessage(), exception);

            aiGatewayMetrics.recordExportDuration(scope, "failed", System.currentTimeMillis() - startMs);

            try {
                exportJob = aiObservabilityExportJobService.getExportJob(exportJobId);

                exportJob.setStatus(AiObservabilityExportJobStatus.FAILED);
                exportJob.setErrorMessage(exception.getMessage());

                aiObservabilityExportJobService.update(exportJob);
            } catch (Exception recoveryException) {
                logger.error(
                    "Failed to mark export job {} as FAILED after primary failure — " +
                        "job may remain stuck in PROCESSING until manual intervention",
                    exportJobId, recoveryException);
            }
        }
    }

    private List<Map<String, Object>> fetchRecords(AiObservabilityExportJob exportJob) {
        AiObservabilityExportScope scope = exportJob.getScope();
        Long workspaceId = exportJob.getWorkspaceId();

        return switch (scope) {
            case TRACES -> {
                Instant end = Instant.now();
                Instant start = end.minusSeconds(30L * 24 * 60 * 60);

                List<AiObservabilityTrace> traces =
                    aiObservabilityTraceService.getTracesByWorkspace(workspaceId, start, end);

                yield traces.stream()
                    .map(trace -> {
                        Map<String, Object> row = new LinkedHashMap<>();

                        row.put("id", trace.getId());
                        row.put("name", trace.getName() != null ? trace.getName() : "");
                        row.put("userId", trace.getUserId() != null ? trace.getUserId() : "");
                        row.put("status", trace.getStatus()
                            .name());
                        row.put("source", trace.getSource()
                            .name());
                        row.put("totalCost", trace.getTotalCost() != null ? trace.getTotalCost()
                            .toString() : "");
                        row.put("totalInputTokens",
                            trace.getTotalInputTokens() != null ? trace.getTotalInputTokens()
                                .toString() : "");
                        row.put("totalOutputTokens",
                            trace.getTotalOutputTokens() != null ? trace.getTotalOutputTokens()
                                .toString() : "");
                        row.put("totalLatencyMs",
                            trace.getTotalLatencyMs() != null ? trace.getTotalLatencyMs()
                                .toString() : "");
                        row.put("createdDate", trace.getCreatedDate()
                            .toString());

                        return row;
                    })
                    .toList();
            }
            case SESSIONS -> {
                List<AiObservabilitySession> sessions =
                    aiObservabilitySessionService.getSessionsByWorkspace(workspaceId);

                yield sessions.stream()
                    .map(session -> {
                        Map<String, Object> row = new LinkedHashMap<>();

                        row.put("id", session.getId());
                        row.put("name", session.getName() != null ? session.getName() : "");
                        row.put("userId", session.getUserId() != null ? session.getUserId() : "");
                        row.put("createdDate", session.getCreatedDate()
                            .toString());

                        return row;
                    })
                    .toList();
            }
            case REQUEST_LOGS -> {
                Instant end = Instant.now();
                Instant start = end.minusSeconds(30L * 24 * 60 * 60);

                List<AiGatewayRequestLog> requestLogs =
                    aiGatewayRequestLogService.getRequestLogsByWorkspace(workspaceId, start, end);

                yield requestLogs.stream()
                    .map(requestLog -> {
                        Map<String, Object> row = new LinkedHashMap<>();

                        row.put("id", requestLog.getId());
                        row.put("createdDate", requestLog.getCreatedDate() != null
                            ? requestLog.getCreatedDate()
                                .toString()
                            : "");
                        row.put("requestedModel",
                            requestLog.getRequestedModel() != null ? requestLog.getRequestedModel() : "");
                        row.put("routedModel",
                            requestLog.getRoutedModel() != null ? requestLog.getRoutedModel() : "");
                        row.put("routedProvider",
                            requestLog.getRoutedProvider() != null ? requestLog.getRoutedProvider() : "");
                        row.put("status",
                            requestLog.getStatus() != null ? requestLog.getStatus()
                                .toString() : "");
                        row.put("latencyMs",
                            requestLog.getLatencyMs() != null ? requestLog.getLatencyMs()
                                .toString() : "");
                        row.put("inputTokens",
                            requestLog.getInputTokens() != null ? requestLog.getInputTokens()
                                .toString() : "");
                        row.put("outputTokens",
                            requestLog.getOutputTokens() != null ? requestLog.getOutputTokens()
                                .toString() : "");
                        row.put("cost",
                            requestLog.getCost() != null ? requestLog.getCost()
                                .toString() : "");
                        row.put("cacheHit", requestLog.isCacheHit());
                        row.put("errorMessage",
                            requestLog.getErrorMessage() != null ? requestLog.getErrorMessage() : "");

                        return row;
                    })
                    .toList();
            }
            case PROMPTS -> {
                List<AiPrompt> prompts = aiPromptService.getPromptsByWorkspace(workspaceId);

                List<Map<String, Object>> rows = new ArrayList<>();

                for (AiPrompt prompt : prompts) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    row.put("id", prompt.getId());
                    row.put("name", prompt.getName() != null ? prompt.getName() : "");
                    row.put("description", prompt.getDescription() != null ? prompt.getDescription() : "");
                    row.put("createdDate", prompt.getCreatedDate() != null
                        ? prompt.getCreatedDate()
                            .toString()
                        : "");

                    List<AiPromptVersion> versions = aiPromptVersionService.getVersionsByPrompt(prompt.getId());

                    List<Map<String, Object>> versionRows = new ArrayList<>();

                    for (AiPromptVersion version : versions) {
                        Map<String, Object> versionRow = new LinkedHashMap<>();

                        versionRow.put("versionNumber", version.getVersionNumber());
                        versionRow.put("environment",
                            version.getEnvironment() != null ? version.getEnvironment() : "");
                        versionRow.put("content", truncateContent(version.getContent()));
                        versionRow.put("active", version.isActive());

                        versionRows.add(versionRow);
                    }

                    row.put("versions", versionRows);

                    rows.add(row);
                }

                yield rows;
            }
        };
    }

    private String truncateContent(String content) {
        if (content == null) {
            return "";
        }

        if (content.length() <= PROMPT_CONTENT_PREVIEW_LENGTH) {
            return content;
        }

        return content.substring(0, PROMPT_CONTENT_PREVIEW_LENGTH);
    }

    private String formatRecords(List<Map<String, Object>> records, AiObservabilityExportFormat format) {
        if (records.isEmpty()) {
            return "";
        }

        return switch (format) {
            case JSON -> JsonUtils.write(records);
            case JSONL -> {
                StringBuilder stringBuilder = new StringBuilder();

                for (Map<String, Object> record : records) {
                    stringBuilder.append(JsonUtils.write(record));
                    stringBuilder.append('\n');
                }

                yield stringBuilder.toString();
            }
            case CSV -> {
                StringBuilder stringBuilder = new StringBuilder();
                List<String> headers = new ArrayList<>(records.getFirst()
                    .keySet());

                stringBuilder.append(String.join(",", headers));
                stringBuilder.append('\n');

                for (Map<String, Object> record : records) {
                    List<String> values = headers.stream()
                        .map(header -> {
                            Object rawValue = record.getOrDefault(header, "");

                            String stringValue;

                            if (rawValue instanceof List<?> || rawValue instanceof Map<?, ?>) {
                                stringValue = JsonUtils.write(rawValue);
                            } else {
                                stringValue = String.valueOf(rawValue);
                            }

                            return escapeCsvValue(stringValue);
                        })
                        .toList();

                    stringBuilder.append(String.join(",", values));
                    stringBuilder.append('\n');
                }

                yield stringBuilder.toString();
            }
        };
    }

    private String escapeCsvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private String generateFilename(AiObservabilityExportJob exportJob) {
        String extension = switch (exportJob.getFormat()) {
            case CSV -> "csv";
            case JSON -> "json";
            case JSONL -> "jsonl";
        };

        return "export_" + exportJob.getScope()
            .name()
            .toLowerCase() + "_" + exportJob.getId() + "." + extension;
    }
}
