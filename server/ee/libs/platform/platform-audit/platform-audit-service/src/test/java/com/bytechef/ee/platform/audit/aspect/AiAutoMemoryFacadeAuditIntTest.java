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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.auto.memory.audit.AiAutoMemoryAuditEvents;
import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipalKey;
import com.bytechef.automation.ai.auto.memory.facade.AiAutoMemoryFacade;
import com.bytechef.automation.ai.auto.memory.facade.AiAutoMemoryFacadeImpl;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    AiAutoMemoryFacadeImpl.class, PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = {
    AiAutoMemoryService.class, ProjectDeploymentFacade.class, ProjectDeploymentService.class, UserService.class,
    WorkspaceFacade.class
})
class AiAutoMemoryFacadeAuditIntTest {

    private static final long MEMORY_ID = 31L;
    private static final long PROJECT_DEPLOYMENT_ID = 42L;
    private static final long USER_ID = 5L;
    private static final long WORKSPACE_ID = 3L;

    @Autowired
    private AiAutoMemoryFacade aiAutoMemoryFacade;

    @Autowired
    private AiAutoMemoryService aiAutoMemoryService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceFacade workspaceFacade;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        User user = new User();

        user.setId(USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(new Workspace(WORKSPACE_ID, "default")));

        authenticate("ROLE_USER");
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateOfOwnMemoryRecordsTheMemoryButNotItsContent() {
        String content = "remember " + UUID.randomUUID();

        AiAutoMemoryPatch aiAutoMemoryPatch = new AiAutoMemoryPatch(null, null, null, content);

        AiAutoMemoryOwner aiAutoMemoryOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, USER_ID, Environment.PRODUCTION);

        when(aiAutoMemoryService.updateById(aiAutoMemoryOwner, MEMORY_ID, 2L, aiAutoMemoryPatch))
            .thenReturn(new AiAutoMemory(aiAutoMemoryOwner));

        aiAutoMemoryFacade.updateAiAutoMemory(
            WORKSPACE_ID, Environment.PRODUCTION, MEMORY_ID, 2L, aiAutoMemoryPatch, null);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiAutoMemoryAuditEvents.AI_AUTO_MEMORY_UPDATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAutoMemoryId", String.valueOf(MEMORY_ID))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environment", "PRODUCTION")
            .doesNotContainKeys("principalType", "principalId", "dataError")
            .doesNotContainValue(content);
    }

    @Test
    void testDeleteOfADeploymentMemoryRecordsWhoseMemoryItWas() {
        authenticate("ROLE_ADMIN");

        AiAutoMemoryOwner aiAutoMemoryOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PROJECT_DEPLOYMENT_ID, Environment.STAGING);

        when(aiAutoMemoryService.deleteById(aiAutoMemoryOwner, MEMORY_ID))
            .thenReturn(new AiAutoMemory(aiAutoMemoryOwner));

        aiAutoMemoryFacade.deleteAiAutoMemory(
            WORKSPACE_ID, Environment.STAGING, MEMORY_ID,
            new AiAutoMemoryPrincipalKey(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PROJECT_DEPLOYMENT_ID));

        verify(aiAutoMemoryService).deleteById(aiAutoMemoryOwner, MEMORY_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiAutoMemoryAuditEvents.AI_AUTO_MEMORY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiAutoMemoryId", String.valueOf(MEMORY_ID))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environment", "STAGING")
            .containsEntry("principalType", "PROJECT_DEPLOYMENT")
            .containsEntry("principalId", String.valueOf(PROJECT_DEPLOYMENT_ID))
            .doesNotContainKey("dataError");
    }

    @Test
    void testDeleteOfAMissingMemoryRecordsAnError() {
        when(aiAutoMemoryService.deleteById(any(AiAutoMemoryOwner.class), eq(MEMORY_ID)))
            .thenThrow(new AiAutoMemoryNotFoundException("Memory not found"));

        assertThatThrownBy(
            () -> aiAutoMemoryFacade.deleteAiAutoMemory(WORKSPACE_ID, Environment.PRODUCTION, MEMORY_ID, null))
                .isInstanceOf(AiAutoMemoryNotFoundException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiAutoMemoryAuditEvents.AI_AUTO_MEMORY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", AiAutoMemoryNotFoundException.class.getName())
            .containsEntry("aiAutoMemoryId", String.valueOf(MEMORY_ID));
    }

    @Test
    void testDeleteInAWorkspaceTheUserDoesNotBelongToRecordsADenial() {
        long otherWorkspaceId = WORKSPACE_ID + 1;

        assertThatThrownBy(
            () -> aiAutoMemoryFacade.deleteAiAutoMemory(otherWorkspaceId, Environment.PRODUCTION, MEMORY_ID, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(aiAutoMemoryService);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiAutoMemoryAuditEvents.AI_AUTO_MEMORY_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "DENIED")
            .containsEntry("workspaceId", String.valueOf(otherWorkspaceId));
    }

    private static void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }
}
