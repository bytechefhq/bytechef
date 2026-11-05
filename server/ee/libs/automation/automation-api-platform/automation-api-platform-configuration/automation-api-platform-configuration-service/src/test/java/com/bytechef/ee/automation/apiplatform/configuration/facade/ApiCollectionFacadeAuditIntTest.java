/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.ee.automation.apiplatform.configuration.audit.ApiCollectionAuditEvents;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfiguration;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ApiCollectionIntTestConfiguration.class, ApiCollectionFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ApiCollectionIntTestConfigurationSharedMocks
class ApiCollectionFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long DEFAULT_WORKSPACE_ID = 1049;
    private static final long UNKNOWN_API_COLLECTION_ID = Long.MAX_VALUE;
    private static final String WORKFLOW_UUID = "4f0d6f0a-6c1f-4d0e-9f53-5d0c8a2e7b11";

    @Autowired
    private ApiCollectionFacade apiCollectionFacade;

    @Autowired
    private ApiCollectionRepository apiCollectionRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    private Project project;

    @BeforeEach
    void beforeEach() {
        project = Project.builder()
            .name("audit-project")
            .workspaceId(DEFAULT_WORKSPACE_ID)
            .build();

        project.publish("v1");

        project = projectRepository.save(project);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        apiEndpointRepository.deleteAll();
        apiCollectionRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateApiCollectionRecordsTheCollectionAndItsDeployment() {
        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("audited-collection"));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ApiCollectionAuditEvents.API_COLLECTION_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiCollectionId", String.valueOf(apiCollectionDTO.id()))
            .containsEntry("name", "audited-collection")
            .containsEntry("projectId", String.valueOf(project.getId()))
            .containsEntry("projectDeploymentId", String.valueOf(apiCollectionDTO.projectDeploymentId()))
            .containsEntry("environment", Environment.DEVELOPMENT.name())
            .doesNotContainKey("dataError");
    }

    @Test
    void testDeleteApiCollectionRecordsTheCollection() {
        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("audited-collection"));

        persistenceAuditEventRepository.deleteAll();

        apiCollectionFacade.deleteApiCollection(apiCollectionDTO.id());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ApiCollectionAuditEvents.API_COLLECTION_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiCollectionId", String.valueOf(apiCollectionDTO.id()));
    }

    @Test
    void testUpdateApiCollectionRecordsTheNewName() {
        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("audited-collection"));

        persistenceAuditEventRepository.deleteAll();

        apiCollectionFacade.updateApiCollection(
            new ApiCollectionDTO(
                apiCollectionDTO.collectionVersion(), apiCollectionDTO.contextPath(), null, null,
                apiCollectionDTO.description(), apiCollectionDTO.enabled(), List.of(), Environment.DEVELOPMENT,
                apiCollectionDTO.id(), null, null, "renamed-collection", null, project.getId(), null,
                apiCollectionDTO.projectDeploymentId(), apiCollectionDTO.projectVersion(), List.of(),
                apiCollectionDTO.version()));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ApiCollectionAuditEvents.API_COLLECTION_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiCollectionId", String.valueOf(apiCollectionDTO.id()))
            .containsEntry("name", "renamed-collection")
            .doesNotContainKey("dataError");
    }

    @Test
    void testEndpointChangesRecordTheEndpointMethodAndPath() {
        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("audited-collection"));

        projectWorkflowRepository.save(
            new ProjectWorkflow(project.getId(), apiCollectionDTO.projectVersion(), "audited-workflow",
                UUID.fromString(WORKFLOW_UUID)));

        persistenceAuditEventRepository.deleteAll();

        ApiCollectionEndpointDTO apiCollectionEndpointDTO = apiCollectionFacade.createApiCollectionEndpoint(
            getApiCollectionEndpointDTO(apiCollectionDTO.id(), null, "audited-endpoint", HttpMethod.GET, 0));

        assertThat(findSingleEvent(ApiCollectionAuditEvents.API_COLLECTION_ENDPOINT_CREATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiCollectionEndpointId", String.valueOf(apiCollectionEndpointDTO.id()))
            .containsEntry("apiCollectionId", String.valueOf(apiCollectionDTO.id()))
            .containsEntry("name", "audited-endpoint")
            .containsEntry("httpMethod", HttpMethod.GET.name())
            .containsEntry("path", "audited-path")
            .doesNotContainKey("dataError");

        apiCollectionFacade.updateApiCollectionEndpoint(
            getApiCollectionEndpointDTO(
                apiCollectionDTO.id(), apiCollectionEndpointDTO.id(), "renamed-endpoint", HttpMethod.POST,
                apiCollectionEndpointDTO.version()));

        assertThat(findSingleEvent(ApiCollectionAuditEvents.API_COLLECTION_ENDPOINT_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiCollectionEndpointId", String.valueOf(apiCollectionEndpointDTO.id()))
            .containsEntry("name", "renamed-endpoint")
            .containsEntry("httpMethod", HttpMethod.POST.name())
            .containsEntry("path", "audited-path")
            .doesNotContainKey("dataError");

        apiCollectionFacade.deleteApiCollectionEndpoint(apiCollectionEndpointDTO.id());

        assertThat(findSingleEvent(ApiCollectionAuditEvents.API_COLLECTION_ENDPOINT_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiCollectionEndpointId", String.valueOf(apiCollectionEndpointDTO.id()));
    }

    @Test
    void testCreateEndpointInAnUnknownCollectionRecordsAnError() {
        assertThatThrownBy(() -> apiCollectionFacade.createApiCollectionEndpoint(
            getApiCollectionEndpointDTO(UNKNOWN_API_COLLECTION_ID, null, "audited-endpoint", HttpMethod.GET, 0)))
                .isInstanceOf(RuntimeException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            ApiCollectionAuditEvents.API_COLLECTION_ENDPOINT_CREATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("apiCollectionId", String.valueOf(UNKNOWN_API_COLLECTION_ID))
            .containsKey("errorClass")
            .doesNotContainKey("apiCollectionEndpointId");
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private ApiCollectionDTO getApiCollectionDTO(String name) {
        return new ApiCollectionDTO(
            1, "audit-context-path", null, null, "audit description", false, List.of(), Environment.DEVELOPMENT,
            null, null, null, name, null, project.getId(), null, 0, 1, List.of(), 0);
    }

    private ApiCollectionEndpointDTO getApiCollectionEndpointDTO(
        long apiCollectionId, Long id, String name, HttpMethod httpMethod, int version) {

        return new ApiCollectionEndpointDTO(
            apiCollectionId, null, null, false, httpMethod, id, null, null, name, "audited-path", 0, version,
            WORKFLOW_UUID);
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
