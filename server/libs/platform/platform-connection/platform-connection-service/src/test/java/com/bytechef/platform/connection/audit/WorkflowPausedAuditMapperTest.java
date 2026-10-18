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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowPausedAuditMapperTest {

    private ConnectionRepository connectionRepository;
    private WorkflowPausedAuditMapper workflowPausedAuditMapper;

    @BeforeEach
    void beforeEach() {
        connectionRepository = mock(ConnectionRepository.class);
        workflowPausedAuditMapper = new WorkflowPausedAuditMapper(connectionRepository);
    }

    @Test
    void testBlockedRunRecordsTheInactiveConnections() {
        when(connectionRepository.findAllByIdIn(List.of(5L, 6L)))
            .thenReturn(List.of(connection(5L, ConnectionStatus.ACTIVE), connection(6L, ConnectionStatus.REVOKED)));

        Map<String, Object> arguments = Map.of(
            "jobPrincipalId", 3L, "workflowId", "workflow-1", "connectionIds", List.of(5L, 6L));

        Object captured = workflowPausedAuditMapper.capture(invocation(arguments, null, AuditOutcome.SUCCESS));

        AuditInvocation auditInvocation = invocation(arguments, captured, AuditOutcome.ERROR);

        assertThat(workflowPausedAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workflowPausedAuditMapper.map(auditInvocation))
            .containsExactlyInAnyOrderEntriesOf(
                Map.of(
                    "jobPrincipalId", "3", "workflowId", "workflow-1", "connectionIds", "6",
                    "connectionStatuses", "REVOKED"));
    }

    @Test
    void testRunWithOnlyActiveConnectionsIsNotRecorded() {
        when(connectionRepository.findAllByIdIn(List.of(5L)))
            .thenReturn(List.of(connection(5L, ConnectionStatus.ACTIVE)));

        Map<String, Object> arguments = Map.of(
            "jobPrincipalId", 3L, "workflowId", "workflow-1", "connectionIds", List.of(5L));

        Object captured = workflowPausedAuditMapper.capture(invocation(arguments, null, AuditOutcome.SUCCESS));

        assertThat(workflowPausedAuditMapper.shouldRecord(invocation(arguments, captured, AuditOutcome.SUCCESS)))
            .isFalse();
    }

    private static Connection connection(long id, ConnectionStatus status) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setStatus(status);

        return connection;
    }

    private static AuditInvocation invocation(
        Map<String, Object> arguments, Object captured, AuditOutcome auditOutcome) {

        return new AuditInvocation(
            WorkflowPausedAuditMapper.WORKFLOW_PAUSED, arguments, null, captured, auditOutcome,
            auditOutcome == AuditOutcome.ERROR ? ConfigurationException.class : null);
    }
}
