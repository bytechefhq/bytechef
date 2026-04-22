/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.llm.usage.service;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmCostEstimator;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageContext;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.llm.usage.WorkspaceAiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.repository.AiLlmUsageRepository;
import com.bytechef.ee.platform.ai.llm.usage.repository.WorkspaceAiLlmUsageRepository;
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
 * Default {@link AiLlmUsageService} + {@link LlmUsageRecorder}. The gateway's facade calls {@link #create(AiLlmUsage)}
 * imperatively to persist a row built via the routing-aware factories on {@link AiLlmUsage}; AI Hub (and future ad-hoc
 * agents) write through the {@link #recordLlm(LlmUsageContext, String, int, int, long)} contract which materialises an
 * {@link AiLlmUsage} with {@code source = AI_HUB} (or other surface) and the routing dims left null.
 *
 * <p>
 * Recording failures from the {@link LlmUsageRecorder} surface are caught + logged + swallowed so a bookkeeping problem
 * can't break the end-user request. The gateway's existing error-handling path (which calls {@link #create(AiLlmUsage)}
 * directly) is unchanged.
 * </p>
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI")
class AiLlmUsageServiceImpl implements AiLlmUsageService, LlmUsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiLlmUsageServiceImpl.class);

    private final AiLlmUsageRepository repository;
    private final WorkspaceAiLlmUsageRepository workspaceRepository;
    private final LlmCostEstimator costEstimator;

    public AiLlmUsageServiceImpl(
        AiLlmUsageRepository repository, WorkspaceAiLlmUsageRepository workspaceRepository,
        LlmCostEstimator costEstimator) {
        this.repository = repository;
        this.workspaceRepository = workspaceRepository;
        this.costEstimator = costEstimator;
    }

    @Override
    public void create(AiLlmUsage usage, Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null — every usage row must claim a workspace via "
            + "workspace_ai_llm_usage so analytics queries (SUM(cost) GROUP BY workspace) see it");

        AiLlmUsage saved = repository.save(usage);

        workspaceRepository.save(new WorkspaceAiLlmUsage(workspaceId, saved.getId()));
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
    public List<AiLlmUsage> getRequestLogs(Instant start, Instant end) {
        return repository.findAllByCreatedDateBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLlmUsage> getRequestLogsByWorkspace(Long workspaceId, Instant start, Instant end) {
        return repository.findAllByWorkspaceIdAndCreatedDateBetween(workspaceId, start, end);
    }

    /**
     * Materialises a row from the {@link LlmUsageRecorder} contract. AI Hub and other shared-recorder callers land
     * here; rows carry {@code source = AI_HUB} (or whatever the context says) and leave the gateway-only routing dims
     * null. The gateway's success/error path doesn't use this — it calls {@link #create(AiLlmUsage)} with a fully-built
     * row from the routing-aware factories.
     *
     * <p>
     * The synthetic {@code requestId} keeps the column NOT-NULLable for every row even when the call didn't route
     * through the gateway. Format: {@code source-prefix:UUID} so a CC-originated row is distinguishable at a glance
     * from a gateway request id (which is the upstream provider's id). Recording failures are caught and swallowed.
     * </p>
     */
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
