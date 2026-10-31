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

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class WorkspaceApiKeyAuditMapperIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WorkspaceApiKeyAuditMapper workspaceApiKeyAuditMapper;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long apiKeyId;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("workspace-api-key-audit"));

        workspaceId = workspace.getId();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long insertedApiKeyId = Objects.requireNonNull(jdbcTemplate.queryForObject(
            "INSERT INTO api_key (name, secret_key, user_id, environment, created_date, created_by, " +
                "last_modified_date, last_modified_by) VALUES ('audit', 'secret', 1, 0, now(), 'test', now(), " +
                "'test') RETURNING id",
            Long.class));

        apiKeyId = insertedApiKeyId;
    }

    @AfterEach
    void afterEach() {
        workspaceApiKeyRepository.deleteAll();
        workspaceRepository.deleteById(workspaceId);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key WHERE id = ?", apiKeyId);
    }

    @Test
    void testCreatedRecordsTheSavedBinding() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        AuditInvocation auditInvocation = invocation(
            WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_CREATED,
            Map.of("apiKeyId", apiKeyId, "workspaceId", workspaceId), workspaceApiKey, null);

        assertThat(workspaceApiKeyAuditMapper.capture(auditInvocation)).isNull();
        assertThat(workspaceApiKeyAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workspaceApiKeyAuditMapper.map(auditInvocation)).containsExactlyInAnyOrderEntriesOf(
            expectedData(workspaceApiKey));
    }

    @Test
    void testDeleteByIdCapturesTheBindingBeforeItIsDeleted() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        Map<String, Object> arguments = Map.of("id", workspaceApiKey.getId());

        Object captured = workspaceApiKeyAuditMapper.capture(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, null));

        workspaceApiKeyRepository.deleteById(workspaceApiKey.getId());

        AuditInvocation auditInvocation = invocation(
            WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, captured);

        assertThat(workspaceApiKeyAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workspaceApiKeyAuditMapper.map(auditInvocation)).containsExactlyInAnyOrderEntriesOf(
            expectedData(workspaceApiKey));
    }

    @Test
    void testDeleteByApiKeyIdCapturesTheBindingBeforeItIsDeleted() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        Map<String, Object> arguments = Map.of("apiKeyId", apiKeyId);

        Object captured = workspaceApiKeyAuditMapper.capture(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, null));

        workspaceApiKeyRepository.deleteById(workspaceApiKey.getId());

        assertThat(workspaceApiKeyAuditMapper.map(
            invocation(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(expectedData(workspaceApiKey));
    }

    @Test
    void testDeleteOfMissingBindingIsNotRecorded() {
        Map<String, Object> arguments = Map.of("id", Long.MAX_VALUE);

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
            WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED, Map.of("id", 5L), null, null,
            AuditOutcome.ERROR, IllegalStateException.class);

        assertThat(workspaceApiKeyAuditMapper.shouldRecord(auditInvocation)).isTrue();
        assertThat(workspaceApiKeyAuditMapper.map(auditInvocation))
            .containsExactly(Map.entry("workspaceApiKeyId", "5"));
    }

    private Map<String, String> expectedData(WorkspaceApiKey workspaceApiKey) {
        return Map.of(
            "workspaceApiKeyId", String.valueOf(workspaceApiKey.getId()), "apiKeyId", String.valueOf(apiKeyId),
            "workspaceId", String.valueOf(workspaceId));
    }

    private static AuditInvocation invocation(
        String event, Map<String, Object> arguments, Object result, Object captured) {

        return new AuditInvocation(event, arguments, result, captured, AuditOutcome.SUCCESS, null);
    }
}
