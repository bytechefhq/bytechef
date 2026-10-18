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

package com.bytechef.platform.connection.audit;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkflowPausedAuditMapper implements AuditMapper {

    public static final String WORKFLOW_PAUSED = "WORKFLOW_PAUSED";

    private static final int MAX_LENGTH = 256;

    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI")
    public WorkflowPausedAuditMapper(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Set<String> events() {
        return Set.of(WORKFLOW_PAUSED);
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        List<?> connectionIds = (List<?>) auditInvocation.requireArgument("connectionIds");

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        return connectionRepository
            .findAllByIdIn(
                connectionIds.stream()
                    .map(connectionId -> ((Number) connectionId).longValue())
                    .toList())
            .stream()
            .filter(connection -> connection.getStatus() != ConnectionStatus.ACTIVE)
            .toList();
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        data.put("jobPrincipalId", String.valueOf(auditInvocation.requireArgument("jobPrincipalId")));
        data.put("workflowId", String.valueOf(auditInvocation.requireArgument("workflowId")));

        if (auditInvocation.captured() instanceof List<?> inactiveConnections) {
            List<Connection> connections = inactiveConnections.stream()
                .map(Connection.class::cast)
                .toList();

            data.put("connectionIds", join(connections.stream()
                .map(connection -> String.valueOf(connection.getId()))
                .toList()));
            data.put("connectionStatuses", join(connections.stream()
                .map(connection -> connection.getStatus()
                    .name())
                .toList()));
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        return auditInvocation.captured() instanceof List<?> inactiveConnections && !inactiveConnections.isEmpty();
    }

    private static String join(List<String> values) {
        String joined = values.stream()
            .collect(Collectors.joining(","));

        return joined.length() > MAX_LENGTH ? joined.substring(0, MAX_LENGTH) : joined;
    }
}
