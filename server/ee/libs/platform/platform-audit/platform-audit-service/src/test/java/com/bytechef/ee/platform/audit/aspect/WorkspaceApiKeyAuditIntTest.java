/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.audit.WorkspaceApiKeyAuditEvents;
import com.bytechef.automation.configuration.audit.WorkspaceApiKeyAuditMapper;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyServiceImpl;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import java.util.List;
import java.util.Optional;
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
@SpringBootTest(classes = WorkspaceApiKeyAuditIntTest.Config.class, properties = "bytechef.edition=ee")
class WorkspaceApiKeyAuditIntTest {

    private static final long API_KEY_ID = 3L;
    private static final long WORKSPACE_API_KEY_ID = 5L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceApiKeyService workspaceApiKeyService;

    @BeforeEach
    void beforeEach() {
        reset(auditEventService, workspaceApiKeyRepository);

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
    void testCreateRecordsTheSavedBinding() {
        WorkspaceApiKey workspaceApiKey = createWorkspaceApiKey();

        when(workspaceApiKeyRepository.save(any(WorkspaceApiKey.class))).thenReturn(workspaceApiKey);

        workspaceApiKeyService.create(API_KEY_ID, WORKSPACE_ID);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceApiKeyId", "5")
            .containsEntry("apiKeyId", "3")
            .containsEntry("workspaceId", "1");
    }

    @Test
    void testDeleteRecordsTheBindingCapturedBeforeTheDelete() {
        WorkspaceApiKey workspaceApiKey = createWorkspaceApiKey();

        when(workspaceApiKeyRepository.findById(WORKSPACE_API_KEY_ID)).thenReturn(Optional.of(workspaceApiKey));

        workspaceApiKeyService.delete(WORKSPACE_API_KEY_ID);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceApiKeyId", "5")
            .containsEntry("apiKeyId", "3")
            .containsEntry("workspaceId", "1");
    }

    @Test
    void testDeleteWorkspaceApiKeyRecordsTheBindingCapturedBeforeTheDelete() {
        WorkspaceApiKey workspaceApiKey = createWorkspaceApiKey();

        when(workspaceApiKeyRepository.findByApiKeyId(API_KEY_ID)).thenReturn(Optional.of(workspaceApiKey));

        workspaceApiKeyService.deleteWorkspaceApiKey(API_KEY_ID);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceApiKeyId", "5")
            .containsEntry("apiKeyId", "3");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteOfMissingBindingRecordsNothing() {
        when(workspaceApiKeyRepository.findById(WORKSPACE_API_KEY_ID)).thenReturn(Optional.empty());

        workspaceApiKeyService.delete(WORKSPACE_API_KEY_ID);

        verify(workspaceApiKeyRepository, never()).deleteById(WORKSPACE_API_KEY_ID);
        verify(auditEventService, never()).saveAll(any(List.class));
    }

    private static WorkspaceApiKey createWorkspaceApiKey() {
        WorkspaceApiKey workspaceApiKey = mock(WorkspaceApiKey.class);

        when(workspaceApiKey.getApiKeyId()).thenReturn(API_KEY_ID);
        when(workspaceApiKey.getId()).thenReturn(WORKSPACE_API_KEY_ID);
        when(workspaceApiKey.getWorkspaceId()).thenReturn(WORKSPACE_ID);

        return workspaceApiKey;
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
        AuditAspect.class, AuditCaptureAspect.class, AuditMapperResolver.class, WorkspaceApiKeyAuditMapper.class,
        WorkspaceApiKeyServiceImpl.class
    })
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }

        @Bean
        WorkspaceApiKeyRepository workspaceApiKeyRepository() {
            return mock(WorkspaceApiKeyRepository.class);
        }
    }
}
