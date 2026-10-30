/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.audit.ApiKeyAuditEvents;
import com.bytechef.platform.security.audit.ApiKeyAuditMapper;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.service.ApiKeyServiceImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ApiKeyAuditIntTest.Config.class, properties = "bytechef.edition=ee")
class ApiKeyAuditIntTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AuditEventService auditEventService;

    @BeforeEach
    void beforeEach() {
        reset(apiKeyRepository, auditEventService);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateRecordsTheSavedApiKeyWithoutItsSecretKey() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey apiKey = invocation.getArgument(0);

            apiKey.setId(7L);

            return apiKey;
        });

        ApiKey createdApiKey = apiKeyService.create(createApiKey());

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ApiKeyAuditEvents.API_KEY_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiKeyId", "7")
            .containsEntry("name", "automation key")
            .containsEntry("type", "AUTOMATION")
            .doesNotContainValue(createdApiKey.getSecretKey());
    }

    @Test
    void testDeleteRecordsTheApiKeyId() {
        apiKeyService.delete(7L);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ApiKeyAuditEvents.API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiKeyId", "7");
    }

    @Test
    void testFailedDeleteRecordsAnErrorAndStillFails() {
        doThrow(new IllegalStateException("boom")).when(apiKeyRepository)
            .deleteById(7L);

        assertThatThrownBy(() -> apiKeyService.delete(7L)).isInstanceOf(IllegalStateException.class);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ApiKeyAuditEvents.API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName())
            .containsEntry("apiKeyId", "7");
    }

    @Test
    void testDeleteWithoutAnAuthenticatedUserIsRecordedUnderSystem() {
        SecurityContextHolder.clearContext();

        apiKeyService.delete(7L);

        assertThat(captureSavedEvent().getPrincipal()).isEqualTo("SYSTEM");
    }

    private static ApiKey createApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("automation key");
        apiKey.setType(PlatformType.AUTOMATION);

        return apiKey;
    }

    @SuppressWarnings("unchecked")
    private PersistentAuditEvent captureSavedEvent() {
        ArgumentCaptor<List<PersistentAuditEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(auditEventService).saveAll(argumentCaptor.capture());

        List<PersistentAuditEvent> persistentAuditEvents = argumentCaptor.getValue();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @Import({
        ApiKeyAuditMapper.class, ApiKeyServiceImpl.class, AuditAspect.class, AuditCaptureAspect.class,
        AuditMapperResolver.class
    })
    static class Config {

        @Bean
        ApiKeyRepository apiKeyRepository() {
            return mock(ApiKeyRepository.class);
        }

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }
    }
}
