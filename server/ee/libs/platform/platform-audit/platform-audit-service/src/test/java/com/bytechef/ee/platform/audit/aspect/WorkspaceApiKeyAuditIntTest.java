/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.audit.WorkspaceApiKeyAuditEvents;
import com.bytechef.automation.configuration.audit.WorkspaceApiKeyAuditMapper;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyServiceImpl;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, WorkspaceApiKeyAuditIntTest.WorkspaceApiKeyRepositoryConfiguration.class,
    WorkspaceApiKeyAuditMapper.class, WorkspaceApiKeyServiceImpl.class
})
class WorkspaceApiKeyAuditIntTest {

    private static final long UNKNOWN_WORKSPACE_API_KEY_ID = Long.MAX_VALUE;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceApiKeyService workspaceApiKeyService;

    private long apiKeyId;
    private long userId;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        userId = insertUser();
        apiKeyId = insertApiKey(userId);
        workspaceId = insertWorkspace();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("DELETE FROM workspace_api_key WHERE workspace_id = ?", workspaceId);
        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", workspaceId);
        jdbcTemplate.update("DELETE FROM api_key WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM \"user\" WHERE id = ?", userId);
    }

    @Test
    void testCreateRecordsTheSavedBinding() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyService.create(apiKeyId, workspaceId);

        Long workspaceApiKeyId = workspaceApiKey.getId();

        assertThat(workspaceApiKeyRepository.findById(Objects.requireNonNull(workspaceApiKeyId))).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceApiKeyId", String.valueOf(workspaceApiKeyId))
            .containsEntry("apiKeyId", String.valueOf(apiKeyId))
            .containsEntry("workspaceId", String.valueOf(workspaceId));
    }

    @Test
    void testDeleteRecordsTheBindingCapturedBeforeTheDelete() {
        long workspaceApiKeyId = saveWorkspaceApiKey();

        workspaceApiKeyService.delete(workspaceApiKeyId);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKeyId)).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceApiKeyId", String.valueOf(workspaceApiKeyId))
            .containsEntry("apiKeyId", String.valueOf(apiKeyId))
            .containsEntry("workspaceId", String.valueOf(workspaceId));
    }

    @Test
    void testDeleteWorkspaceApiKeyRecordsTheBindingCapturedBeforeTheDelete() {
        long workspaceApiKeyId = saveWorkspaceApiKey();

        workspaceApiKeyService.deleteWorkspaceApiKey(apiKeyId);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKeyId)).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceApiKeyId", String.valueOf(workspaceApiKeyId))
            .containsEntry("apiKeyId", String.valueOf(apiKeyId));
    }

    @Test
    void testDeleteOfMissingBindingRecordsNothing() {
        long workspaceApiKeyId = saveWorkspaceApiKey();

        workspaceApiKeyService.delete(UNKNOWN_WORKSPACE_API_KEY_ID);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKeyId)).isPresent();
        assertThat(findAllEvents()).isEmpty();
    }

    private List<PersistentAuditEvent> findAllEvents() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        return page.getContent();
    }

    private PersistentAuditEvent findSingleEvent() {
        List<PersistentAuditEvent> persistentAuditEvents = findAllEvents();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private long insertApiKey(long apiKeyUserId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO api_key
                        (name, secret_key, user_id, type, environment, created_by, created_date, last_modified_by,
                         last_modified_date)
                    VALUES ('workspace key', 'secret', ?, 0, 0, 'test', now(), 'test', now())
                    RETURNING id
                    """,
                Long.class, apiKeyUserId),
            "api key id");
    }

    private long insertUser() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO "user"
                        (login, password_hash, email, activated, uuid, created_by, created_date, last_modified_by,
                         last_modified_date)
                    VALUES ('workspace-api-key-audit', 'n/a', 'workspace-api-key-audit@example.com', true,
                        gen_random_uuid(), 'test', now(), 'test', now())
                    RETURNING id
                    """,
                Long.class),
            "user id");
    }

    private long insertWorkspace() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                    VALUES ('workspace api key audit', now(), 'test', now(), 'test', 1)
                    RETURNING id
                    """,
                Long.class),
            "workspace id");
    }

    private long saveWorkspaceApiKey() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        return Objects.requireNonNull(workspaceApiKey.getId());
    }

    @Configuration
    @EnableJdbcRepositories(
        basePackageClasses = WorkspaceApiKeyRepository.class,
        includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WorkspaceApiKeyRepository.class))
    static class WorkspaceApiKeyRepositoryConfiguration {
    }
}
