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

package com.bytechef.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkspaceApiKeyAuditMapperTest {

    private static final long API_KEY_ID = 3L;
    private static final long WORKSPACE_API_KEY_ID = 5L;
    private static final long WORKSPACE_ID = 1L;

    private WorkspaceApiKeyAuditMapper workspaceApiKeyAuditMapper;
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @BeforeEach
    void beforeEach() {
        workspaceApiKeyRepository = mock(WorkspaceApiKeyRepository.class);
        workspaceApiKeyAuditMapper = new WorkspaceApiKeyAuditMapper(workspaceApiKeyRepository);
    }

    @Test
    void testCreatedRecordsTheSavedBinding() {
        AuditInvocation auditInvocation = invocation(
            WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_CREATED,
            Map.of("apiKeyId", API_KEY_ID, "workspaceId", WORKSPACE_ID), createWorkspaceApiKey(), null);

        assertThat(workspaceApiKeyAuditMapper.capture(auditInvocation)).isNull();
        assertThat(workspaceApiKeyAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workspaceApiKeyAuditMapper.map(auditInvocation)).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceApiKeyId", "5", "apiKeyId", "3", "workspaceId", "1"));

        verifyNoInteractions(workspaceApiKeyRepository);
    }

    @Test
    void testDeleteByIdCapturesTheBindingBeforeItIsDeleted() {
        WorkspaceApiKey workspaceApiKey = createWorkspaceApiKey();

        when(workspaceApiKeyRepository.findById(WORKSPACE_API_KEY_ID)).thenReturn(Optional.of(workspaceApiKey));

        Map<String, Object> arguments = Map.of("id", WORKSPACE_API_KEY_ID);

        Object captured = workspaceApiKeyAuditMapper.capture(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, null));

        AuditInvocation auditInvocation = invocation(
            WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, captured);

        assertThat(workspaceApiKeyAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workspaceApiKeyAuditMapper.map(auditInvocation)).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceApiKeyId", "5", "apiKeyId", "3", "workspaceId", "1"));
    }

    @Test
    void testDeleteByApiKeyIdCapturesTheBindingBeforeItIsDeleted() {
        WorkspaceApiKey workspaceApiKey = createWorkspaceApiKey();

        when(workspaceApiKeyRepository.findByApiKeyId(API_KEY_ID)).thenReturn(Optional.of(workspaceApiKey));

        Map<String, Object> arguments = Map.of("apiKeyId", API_KEY_ID);

        Object captured = workspaceApiKeyAuditMapper.capture(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, null));

        assertThat(workspaceApiKeyAuditMapper.map(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of("workspaceApiKeyId", "5", "apiKeyId", "3", "workspaceId", "1"));
    }

    @Test
    void testDeleteOfMissingBindingIsNotRecorded() {
        when(workspaceApiKeyRepository.findById(WORKSPACE_API_KEY_ID)).thenReturn(Optional.empty());

        Map<String, Object> arguments = Map.of("id", WORKSPACE_API_KEY_ID);

        Object captured = workspaceApiKeyAuditMapper.capture(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, null));

        assertThat(captured).isNull();
        assertThat(workspaceApiKeyAuditMapper.shouldRecord(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, captured)))
                .isFalse();
    }

    @Test
    void testFailedDeleteIsRecordedWithItsArguments() {
        AuditInvocation auditInvocation = new AuditInvocation(
            WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, Map.of("id", WORKSPACE_API_KEY_ID), null, null,
            AuditOutcome.ERROR, IllegalStateException.class);

        assertThat(workspaceApiKeyAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workspaceApiKeyAuditMapper.map(auditInvocation))
            .containsExactly(Map.entry("workspaceApiKeyId", "5"));
    }

    private static WorkspaceApiKey createWorkspaceApiKey() {
        WorkspaceApiKey workspaceApiKey = mock(WorkspaceApiKey.class);

        when(workspaceApiKey.getApiKeyId()).thenReturn(API_KEY_ID);
        when(workspaceApiKey.getId()).thenReturn(WORKSPACE_API_KEY_ID);
        when(workspaceApiKey.getWorkspaceId()).thenReturn(WORKSPACE_ID);

        return workspaceApiKey;
    }

    private static AuditInvocation invocation(
        String event, Map<String, Object> arguments, Object result, Object captured) {

        return new AuditInvocation(event, arguments, result, captured, AuditOutcome.SUCCESS, null);
    }
}
