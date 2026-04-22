/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 */
public interface AiGatewayRequestLogService {

    void create(AiGatewayRequestLog requestLog);

    void deleteOlderThan(Instant date);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    List<Long> findDistinctWorkspaceIds();

    Map<String, Double> getAverageLatencyByModel(Instant since);

    List<AiGatewayRequestLog> getRequestLogs(Instant start, Instant end);

    List<AiGatewayRequestLog> getRequestLogsByWorkspace(Long workspaceId, Instant start, Instant end);

    List<AiGatewayRequestLog> getRequestLogsByWorkspaceAndProperty(
        Long workspaceId, Instant start, Instant end, String propertyKey, String propertyValue);
}
