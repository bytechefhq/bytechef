/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.audit.WorkspaceAuditEvents;
import com.bytechef.ee.automation.configuration.config.WorkspacePermissionIntTestConfiguration;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.service.WorkspaceServiceImpl;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        WorkspacePermissionIntTestConfiguration.class, AdminWorkspaceFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
class AdminWorkspaceFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";

    private final List<Long> workspaceIds = new ArrayList<>();

    @Autowired
    private AdminWorkspaceFacade adminWorkspaceFacade;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.setContext(
            new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(
                    ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN)))));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        workspaceIds.forEach(workspaceRepository::deleteById);

        workspaceIds.clear();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateWorkspaceRecordsTheWorkspace() {
        String name = "workspace-" + UUID.randomUUID();

        Workspace workspace = adminWorkspaceFacade.createWorkspace(new Workspace(name));

        workspaceIds.add(workspace.getId());

        assertThat(findSingleEvent(WorkspaceAuditEvents.WORKSPACE_CREATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceId", String.valueOf(workspace.getId()))
            .containsEntry("name", name);
    }

    @Test
    void testCreateOfAWorkspaceWithAnIdRecordsAFailure() {
        Workspace workspace = new Workspace(Long.MAX_VALUE, "workspace-with-id");

        assertThatThrownBy(() -> adminWorkspaceFacade.createWorkspace(workspace))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(findSingleEvent(WorkspaceAuditEvents.WORKSPACE_CREATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("name", "workspace-with-id")
            .doesNotContainKey("workspaceId");
    }

    @Test
    void testUpdateWorkspaceRecordsTheRenamedWorkspace() {
        Workspace workspace = saveWorkspace();

        Workspace renamedWorkspace = new Workspace(workspace.getId(), "renamed");

        renamedWorkspace.setVersion(workspace.getVersion());

        adminWorkspaceFacade.updateWorkspace(renamedWorkspace);

        assertThat(adminWorkspaceFacade.getWorkspace(workspace.getId())
            .getName()).isEqualTo("renamed");

        assertThat(findSingleEvent(WorkspaceAuditEvents.WORKSPACE_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceId", String.valueOf(workspace.getId()))
            .containsEntry("name", "renamed");
    }

    @Test
    void testUpdateOfAnUnknownWorkspaceRecordsAFailure() {
        Workspace workspace = new Workspace(Long.MAX_VALUE, "unknown");

        assertThatThrownBy(() -> adminWorkspaceFacade.updateWorkspace(workspace))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(WorkspaceAuditEvents.WORKSPACE_UPDATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("workspaceId", String.valueOf(Long.MAX_VALUE))
            .containsEntry("name", "unknown");
    }

    @Test
    void testDeleteWorkspaceRecordsTheWorkspaceId() {
        Workspace workspace = saveWorkspace();

        adminWorkspaceFacade.deleteWorkspace(workspace.getId());

        assertThat(workspaceRepository.findById(workspace.getId())).isEmpty();

        assertThat(findSingleEvent(WorkspaceAuditEvents.WORKSPACE_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceId", String.valueOf(workspace.getId()));
    }

    @Test
    void testDeleteOfAnUnknownWorkspaceRecordsAFailure() {
        assertThatThrownBy(() -> adminWorkspaceFacade.deleteWorkspace(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(WorkspaceAuditEvents.WORKSPACE_DELETED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("workspaceId", String.valueOf(Long.MAX_VALUE));
    }

    private Workspace saveWorkspace() {
        Workspace workspace = workspaceRepository.save(new Workspace("workspace-" + UUID.randomUUID()));

        workspaceIds.add(workspace.getId());

        return workspace;
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 100));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);

        return persistentAuditEvent;
    }

    @Configuration
    @Import({
        AdminWorkspaceFacadeImpl.class, AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class,
        AuditMapperResolver.class, WorkspaceServiceImpl.class
    })
    static class AuditConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }
    }
}
