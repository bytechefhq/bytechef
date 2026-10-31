/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.audit.ApiKeyAuditEvents;
import com.bytechef.platform.security.audit.ApiKeyAuditMapper;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.service.ApiKeyServiceImpl;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    ApiKeyAuditMapper.class, ApiKeyServiceImpl.class, PostgreSQLContainerConfiguration.class
})
class ApiKeyAuditIntTest {

    private static final long UNKNOWN_API_KEY_ID = Long.MAX_VALUE;

    @MockitoSpyBean
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private long userId;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        userId = insertUser();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("DELETE FROM api_key WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM \"user\" WHERE id = ?", userId);
    }

    @Test
    void testCreateRecordsTheSavedApiKeyWithoutItsSecretKey() {
        ApiKey createdApiKey = apiKeyService.create(createApiKey(PlatformType.AUTOMATION));

        Long createdApiKeyId = createdApiKey.getId();

        assertThat(apiKeyRepository.findById(Objects.requireNonNull(createdApiKeyId))).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ApiKeyAuditEvents.API_KEY_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiKeyId", String.valueOf(createdApiKeyId))
            .containsEntry("name", "automation key")
            .containsEntry("type", "AUTOMATION")
            .doesNotContainValue(createdApiKey.getSecretKey());
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testDeleteRecordsThePlatformOfTheDeletedApiKey() {
        long apiKeyId = saveApiKey(PlatformType.EMBEDDED);

        apiKeyService.delete(apiKeyId);

        assertThat(apiKeyRepository.findById(apiKeyId)).isEmpty();
        assertThat(findSingleEvent().getPlatform()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testDeleteOfAnUnknownApiKeyHasNoPlatform() {
        apiKeyService.delete(UNKNOWN_API_KEY_ID);

        assertThat(findSingleEvent().getPlatform()).isNull();
    }

    @Test
    void testDeleteRecordsTheApiKeyId() {
        long apiKeyId = saveApiKey(PlatformType.AUTOMATION);

        apiKeyService.delete(apiKeyId);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ApiKeyAuditEvents.API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiKeyId", String.valueOf(apiKeyId));
    }

    @Test
    void testFailedDeleteRecordsAnErrorAndStillFails() {
        long apiKeyId = saveApiKey(PlatformType.AUTOMATION);

        doThrow(new IllegalStateException("boom")).when(apiKeyRepository)
            .deleteById(apiKeyId);

        assertThatThrownBy(() -> apiKeyService.delete(apiKeyId)).isInstanceOf(IllegalStateException.class);

        assertThat(apiKeyRepository.findById(apiKeyId)).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ApiKeyAuditEvents.API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName())
            .containsEntry("apiKeyId", String.valueOf(apiKeyId));
    }

    @Test
    void testDeleteWithoutAnAuthenticatedUserIsRecordedUnderSystem() {
        long apiKeyId = saveApiKey(PlatformType.AUTOMATION);

        SecurityContextHolder.clearContext();

        apiKeyService.delete(apiKeyId);

        assertThat(findSingleEvent().getPrincipal()).isEqualTo("SYSTEM");
    }

    private ApiKey createApiKey(PlatformType platformType) {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("automation key");
        apiKey.setType(platformType);
        apiKey.setUserId(userId);

        return apiKey;
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private long insertUser() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO "user"
                        (login, password_hash, email, activated, uuid, created_by, created_date, last_modified_by,
                         last_modified_date)
                    VALUES ('api-key-audit', 'n/a', 'api-key-audit@example.com', true, gen_random_uuid(), 'test',
                        now(), 'test', now())
                    RETURNING id
                    """,
                Long.class),
            "user id");
    }

    private long saveApiKey(PlatformType platformType) {
        ApiKey apiKey = createApiKey(platformType);

        apiKey.setSecretKey(String.valueOf(UUID.randomUUID()));

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        return Objects.requireNonNull(savedApiKey.getId());
    }
}
