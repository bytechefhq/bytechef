/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayRequestLogRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiGatewayRequestLogServiceImpl implements AiGatewayRequestLogService {

    private final AiGatewayRequestLogRepository aiGatewayRequestLogRepository;

    public AiGatewayRequestLogServiceImpl(
        AiGatewayRequestLogRepository aiGatewayRequestLogRepository) {

        this.aiGatewayRequestLogRepository = aiGatewayRequestLogRepository;
    }

    @Override
    public void create(AiGatewayRequestLog requestLog) {
        aiGatewayRequestLogRepository.save(requestLog);
    }

    @Override
    public void deleteOlderThan(Instant date) {
        aiGatewayRequestLogRepository.deleteAllByCreatedDateBefore(date);
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        aiGatewayRequestLogRepository.deleteAllByWorkspaceIdAndCreatedDateBefore(workspaceId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDistinctWorkspaceIds() {
        return aiGatewayRequestLogRepository.findDistinctWorkspaceIds();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getAverageLatencyByModel(Instant since) {
        List<AiGatewayRequestLog> recentLogs =
            aiGatewayRequestLogRepository.findAllByStatusAndCreatedDateAfter(200, since);

        return recentLogs.stream()
            .filter(log -> log.getRoutedModel() != null && log.getLatencyMs() != null)
            .collect(Collectors.groupingBy(
                AiGatewayRequestLog::getRoutedModel,
                Collectors.averagingInt(AiGatewayRequestLog::getLatencyMs)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRequestLog> getRequestLogs(Instant start, Instant end) {
        return aiGatewayRequestLogRepository.findAllByCreatedDateBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRequestLog> getRequestLogsByWorkspace(Long workspaceId, Instant start, Instant end) {
        return aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCreatedDateBetween(workspaceId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayRequestLog> getRequestLogsByWorkspaceAndProperty(
        Long workspaceId, Instant start, Instant end, String propertyKey, String propertyValue) {

        return aiGatewayRequestLogRepository.findAllByWorkspaceIdAndCustomProperty(
            workspaceId, start, end, propertyKey, propertyValue);
    }
}
