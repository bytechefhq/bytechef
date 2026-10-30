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

package com.bytechef.automation.ai.mcp.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpProjectAuditMapperTest {

    private static final Map<String, Object> CREATE_ARGUMENTS = Map.of(
        "mcpServerId", 2L, "projectId", 3L, "projectVersion", 1, "selectedWorkflowIds", List.of("workflow1"));

    private final McpProjectAuditMapper mcpProjectAuditMapper = new McpProjectAuditMapper();

    @Test
    void testCreatedRecordsTheMcpProjectAndItsSource() {
        Map<String, String> data = mcpProjectAuditMapper.map(new AuditInvocation(
            McpProjectAuditEvents.MCP_PROJECT_CREATED, CREATE_ARGUMENTS, new McpProject(7L, 11L, 2L), null,
            AuditOutcome.SUCCESS, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("mcpProjectId", "7", "mcpServerId", "2", "projectId", "3", "projectVersion", "1"));
    }

    @Test
    void testDeniedCreateRecordsItsArgumentsWithoutAnId() {
        Map<String, String> data = mcpProjectAuditMapper.map(new AuditInvocation(
            McpProjectAuditEvents.MCP_PROJECT_CREATED, CREATE_ARGUMENTS, null, null, AuditOutcome.DENIED, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("mcpServerId", "2", "projectId", "3", "projectVersion", "1"));
    }

    @Test
    void testDeletedRecordsId() {
        Map<String, String> data = mcpProjectAuditMapper.map(new AuditInvocation(
            McpProjectAuditEvents.MCP_PROJECT_DELETED, Map.of("mcpProjectId", 7L), null, null, AuditOutcome.SUCCESS,
            null));

        assertThat(data).containsExactly(Map.entry("mcpProjectId", "7"));
    }

    @Test
    void testRenamedArgumentFailsMappingInsteadOfRecordingNull() {
        AuditInvocation auditInvocation = new AuditInvocation(
            McpProjectAuditEvents.MCP_PROJECT_DELETED, Map.of("id", 7L), null, null, AuditOutcome.SUCCESS, null);

        assertThatThrownBy(() -> mcpProjectAuditMapper.map(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mcpProjectId");
    }
}
