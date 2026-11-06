/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage.service;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmCostEstimator;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageContext;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import com.bytechef.ee.platform.ai.llm.usage.repository.AiLlmUsageRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI")
class AiLlmUsageServiceImpl implements AiLlmUsageService, LlmUsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiLlmUsageServiceImpl.class);

    private final AiLlmUsageRepository repository;
    private final LlmCostEstimator costEstimator;

    public AiLlmUsageServiceImpl(AiLlmUsageRepository repository, LlmCostEstimator costEstimator) {
        this.repository = repository;
        this.costEstimator = costEstimator;
    }

    @Override
    public void create(AiLlmUsage usage, Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null — every usage row must claim a workspace so "
            + "analytics queries (SUM(cost) GROUP BY workspace) see it");

        usage.setWorkspaceId(workspaceId);

        repository.save(usage);
    }

    @Override
    public void createForConnectedUser(AiLlmUsage usage, long connectedUserId) {
        usage.setWorkspaceId(null);
        usage.setUserId(connectedUserId);

        repository.save(usage);
    }

    @Override
    public void deleteOlderThan(Instant date) {
        repository.deleteAllByCreatedDateBefore(date);
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        repository.deleteAllByWorkspaceIdAndCreatedDateBefore(workspaceId, date);
    }

    @Override
    public void deleteOlderThanWithoutWorkspace(Instant date) {
        Validate.notNull(date, "date must not be null");

        repository.deleteAllByWorkspaceIdIsNullAndCreatedDateBefore(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDistinctWorkspaceIds() {
        return repository.findDistinctWorkspaceIds();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getAverageLatencyByModel(Instant since) {
        List<AiLlmUsage> recent = repository.findAllByStatusAndCreatedDateAfter(200, since);

        return recent.stream()
            .filter(row -> row.getRoutedModel() != null && row.getLatencyMs() != null)
            .collect(Collectors.groupingBy(
                AiLlmUsage::getRoutedModel,
                Collectors.averagingInt(AiLlmUsage::getLatencyMs)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmUsage> getUsagesByOwner(LlmUsageSource source, long ownerId) {
        return repository.findAllBySourceAndOwnerId(source.ordinal(), ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmUsage> getRequestLogs(Instant start, Instant end) {
        return repository.findAllByCreatedDateBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmUsage> getRequestLogsByWorkspace(Long workspaceId, Instant start, Instant end) {
        return repository.findAllByWorkspaceIdAndCreatedDateBetween(workspaceId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmUsage> getConnectedUserRequestLogsWithoutWorkspace(Instant start, Instant end) {
        return repository.findAllByWorkspaceIdIsNullAndUserIdIsNotNullAndCreatedDateBetween(start, end);
    }

    @Override
    public void recordLlm(LlmUsageContext context, String model, int inputTokens, int outputTokens, long durationMs) {
        try {
            int safeInput = Math.max(inputTokens, 0);
            int safeOutput = Math.max(outputTokens, 0);
            int safeDuration = (int) Math.min(Math.max(durationMs, 0), Integer.MAX_VALUE);
            BigDecimal cost = costEstimator.estimateLlmCost(model != null ? model : "", safeInput, safeOutput);

            String requestId = context.source()
                .name() + ":" + UUID.randomUUID();

            AiLlmUsage usage = new AiLlmUsage(requestId, model != null ? model : "");

            usage.setUserId(context.userId());
            usage.setOwnerId(context.ownerId());
            usage.setSource(context.source());
            usage.setAgentName(context.agentName());
            usage.setParentAgent(context.parentAgent());
            usage.setInputTokens(safeInput);
            usage.setOutputTokens(safeOutput);
            usage.setLatencyMs(safeDuration);

            if (cost != null && cost.signum() >= 0) {
                usage.setCost(cost);
            }

            create(usage, context.workspaceId());
        } catch (RuntimeException exception) {
            log.error(
                "Failed to record LLM usage for workspace={}, source={}, model={}: {}",
                context.workspaceId(), context.source(), model, exception.getMessage(), exception);
        }
    }
}
