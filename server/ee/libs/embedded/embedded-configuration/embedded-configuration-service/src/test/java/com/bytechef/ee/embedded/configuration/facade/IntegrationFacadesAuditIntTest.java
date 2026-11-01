/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.audit.AppEventAuditEvents;
import com.bytechef.ee.embedded.configuration.audit.IntegrationAuditEvents;
import com.bytechef.ee.embedded.configuration.audit.WorkflowAuditEvents;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.repository.AppEventRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
        IntegrationIntTestConfiguration.class, IntegrationFacadesAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationFacadesAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String PERMISSION_EXPRESSION = "metadata['tier'] == 'pro'";

    @Autowired
    private AppEventFacade appEventFacade;

    @Autowired
    private AppEventRepository appEventRepository;

    @Autowired
    private ConnectedUserIntegrationFacade connectedUserIntegrationFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceFacade integrationInstanceFacade;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkflowService workflowService;

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

        appEventRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
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
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);

        return persistentAuditEvent;
    }

    private Integration createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        return integrationService.create(integration);
    }

    private Integration createPublishedIntegration() {
        Integration integration = createIntegration();

        integrationService.publishIntegration(integration.getId(), null);

        return integrationService.getIntegration(integration.getId());
    }

    private IntegrationInstanceConfiguration createIntegrationInstanceConfiguration() {
        Integration integration = createPublishedIntegration();

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of("key", "value"));
        integrationInstanceConfiguration.setEnvironment(Environment.STAGING);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        return integrationInstanceConfigurationService.create(integrationInstanceConfiguration);
    }

    private static IntegrationInstanceConfigurationDTO newIntegrationInstanceConfigurationDTO(
        long integrationId, String name) {

        return IntegrationInstanceConfigurationDTO.builder()
            .connectionParameters(Map.of("key", "value"))
            .environment(Environment.STAGING)
            .integrationId(integrationId)
            .integrationInstanceConfigurationWorkflows(List.of())
            .integrationVersion(1)
            .name(name)
            .tags(List.of())
            .build();
    }

    @Nested
    class IntegrationFacadeAudit {

        @Test
        void testCreateIntegrationRecordsTheIntegration() {
            String name = "integration-" + UUID.randomUUID();

            IntegrationDTO integrationDTO = IntegrationDTO.builder()
                .componentName("componentName")
                .name(name)
                .build();

            long integrationId = integrationFacade.createIntegration(integrationDTO);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_CREATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationId", String.valueOf(integrationId))
                .containsEntry("name", name)
                .containsEntry("componentName", "componentName");
        }

        @Test
        void testUpdateIntegrationRecordsTheRenamedIntegration() {
            Integration integration = createIntegration();

            IntegrationDTO integrationDTO = IntegrationDTO.builder()
                .componentName(integration.getComponentName())
                .id(integration.getId())
                .name("renamed")
                .version(integration.getVersion())
                .build();

            integrationFacade.updateIntegration(integrationDTO);

            assertThat(integrationService.getIntegration(integration.getId())
                .getName()).isEqualTo("renamed");

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_UPDATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationId", String.valueOf(integration.getId()))
                .containsEntry("name", "renamed");
        }

        @Test
        void testUpdateOfAnUnknownIntegrationRecordsAFailure() {
            IntegrationDTO integrationDTO = IntegrationDTO.builder()
                .componentName("componentName")
                .id(Long.MAX_VALUE)
                .name("unknown")
                .build();

            assertThatThrownBy(() -> integrationFacade.updateIntegration(integrationDTO))
                .isInstanceOf(NoSuchElementException.class);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_UPDATED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", NoSuchElementException.class.getName())
                .containsEntry("integrationId", String.valueOf(Long.MAX_VALUE))
                .containsEntry("name", "unknown");
        }

        @Test
        void testPublishIntegrationRecordsTheIntegrationIdAndThePublishedVersion() {
            Integration integration = createIntegration();

            int integrationVersion = integrationFacade.publishIntegration(integration.getId(), "first release");

            assertThat(integrationService.getIntegration(integration.getId())
                .isPublished()).isTrue();

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_PUBLISHED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationId", String.valueOf(integration.getId()))
                .containsEntry("integrationVersion", String.valueOf(integrationVersion));
        }

        @Test
        void testPublishOfAnUnknownIntegrationRecordsAFailure() {
            assertThatThrownBy(() -> integrationFacade.publishIntegration(Long.MAX_VALUE, null))
                .isInstanceOf(NoSuchElementException.class);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_PUBLISHED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", NoSuchElementException.class.getName())
                .containsEntry("integrationId", String.valueOf(Long.MAX_VALUE))
                .doesNotContainKey("integrationVersion");
        }

        @Test
        void testDeleteIntegrationRecordsTheIntegrationId() {
            Integration integration = createIntegration();

            integrationFacade.deleteIntegration(integration.getId());

            assertThat(integrationRepository.findById(integration.getId())).isEmpty();

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_DELETED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationId", String.valueOf(integration.getId()));
        }

        @Test
        void testDeleteOfAnUnknownIntegrationRecordsAFailure() {
            assertThatThrownBy(() -> integrationFacade.deleteIntegration(Long.MAX_VALUE))
                .isInstanceOf(NoSuchElementException.class);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_DELETED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", NoSuchElementException.class.getName())
                .containsEntry("integrationId", String.valueOf(Long.MAX_VALUE));
        }
    }

    @Nested
    class IntegrationInstanceConfigurationFacadeAudit {

        @Test
        void testCreateIntegrationInstanceConfigurationRecordsTheConfiguration() {
            Integration integration = createPublishedIntegration();

            String name = "instance-configuration-" + UUID.randomUUID();

            long integrationInstanceConfigurationId =
                integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                    newIntegrationInstanceConfigurationDTO(integration.getId(), name));

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_CREATED).getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "integrationInstanceConfigurationId", String.valueOf(integrationInstanceConfigurationId))
                    .containsEntry("integrationId", String.valueOf(integration.getId()))
                    .containsEntry("name", name)
                    .containsEntry("environment", Environment.STAGING.name());
        }

        @Test
        void testCreateForAnUnpublishedIntegrationRecordsAFailure() {
            Integration integration = createIntegration();

            String name = "instance-configuration-" + UUID.randomUUID();

            assertThatThrownBy(
                () -> integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                    newIntegrationInstanceConfigurationDTO(integration.getId(), name)))
                        .isInstanceOf(ConfigurationException.class);

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_CREATED).getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("errorClass", ConfigurationException.class.getName())
                    .containsEntry("integrationId", String.valueOf(integration.getId()))
                    .containsEntry("name", name)
                    .containsEntry("environment", Environment.STAGING.name())
                    .doesNotContainKey("integrationInstanceConfigurationId");
        }

        @Test
        void testUpdateIntegrationInstanceConfigurationRecordsTheRenamedConfiguration() {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                createIntegrationInstanceConfiguration();

            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
                newIntegrationInstanceConfigurationDTO(integrationInstanceConfiguration.getIntegrationId(), "renamed")
                    .toBuilder()
                    .id(integrationInstanceConfiguration.getId())
                    .integrationId(integrationInstanceConfiguration.getIntegrationId())
                    .integrationInstanceConfigurationWorkflows(List.of())
                    .integrationVersion(1)
                    .tags(List.of())
                    .version(integrationInstanceConfiguration.getVersion())
                    .build();

            integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
                integrationInstanceConfigurationDTO);

            assertThat(
                integrationInstanceConfigurationService
                    .getIntegrationInstanceConfiguration(integrationInstanceConfiguration.getId())
                    .getName()).isEqualTo("renamed");

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_UPDATED).getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "integrationInstanceConfigurationId",
                        String.valueOf(integrationInstanceConfiguration.getId()))
                    .containsEntry(
                        "integrationId", String.valueOf(integrationInstanceConfiguration.getIntegrationId()))
                    .containsEntry("name", "renamed");
        }

        @Test
        void testEnableIntegrationInstanceConfigurationRecordsTheNewState() {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                createIntegrationInstanceConfiguration();

            integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(
                integrationInstanceConfiguration.getId(), true);

            assertThat(
                integrationInstanceConfigurationService
                    .getIntegrationInstanceConfiguration(integrationInstanceConfiguration.getId())
                    .isEnabled()).isTrue();

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_ENABLED_UPDATED).getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "integrationInstanceConfigurationId",
                        String.valueOf(integrationInstanceConfiguration.getId()))
                    .containsEntry("enabled", "true");
        }

        @Test
        void testEnableIntegrationInstanceConfigurationWorkflowRecordsTheWorkflowAndTheNewState() {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                createIntegrationInstanceConfiguration();

            String workflowId = String.valueOf(UUID.randomUUID());

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setEnabled(true);
            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfiguration.getId());
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

            integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
                integrationInstanceConfigurationWorkflow);

            integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfiguration.getId(), workflowId, false);

            assertThat(
                integrationInstanceConfigurationWorkflowService
                    .getIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationWorkflow.getId())
                    .isEnabled()).isFalse();

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED)
                    .getData())
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "integrationInstanceConfigurationId",
                            String.valueOf(integrationInstanceConfiguration.getId()))
                        .containsEntry("workflowId", workflowId)
                        .containsEntry("enabled", "false");
        }

        @Test
        void testDeleteIntegrationInstanceConfigurationRecordsTheConfigurationId() {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                createIntegrationInstanceConfiguration();

            integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(
                integrationInstanceConfiguration.getId());

            assertThat(integrationInstanceConfigurationRepository.findById(integrationInstanceConfiguration.getId()))
                .isEmpty();

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_DELETED).getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "integrationInstanceConfigurationId",
                        String.valueOf(integrationInstanceConfiguration.getId()));
        }

        @Test
        void testDeleteOfAnUnknownIntegrationInstanceConfigurationRecordsAFailure() {
            assertThatThrownBy(
                () -> integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(Long.MAX_VALUE))
                    .isInstanceOf(NoSuchElementException.class);

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_CONFIGURATION_DELETED).getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("errorClass", NoSuchElementException.class.getName())
                    .containsEntry("integrationInstanceConfigurationId", String.valueOf(Long.MAX_VALUE));
        }
    }

    @Nested
    class AppEventFacadeAudit {

        @Test
        void testCreateUpdateAndDeleteRecordTheAppEvent() {
            AppEvent appEvent = new AppEvent();

            appEvent.setName("order-created");
            appEvent.setSchema("{}");

            AppEvent createdAppEvent = appEventFacade.create(appEvent);

            assertThat(findSingleEvent(AppEventAuditEvents.APP_EVENT_CREATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("appEventId", String.valueOf(createdAppEvent.getId()))
                .containsEntry("name", "order-created");

            createdAppEvent.setName("order-placed");

            appEventFacade.update(createdAppEvent);

            assertThat(findSingleEvent(AppEventAuditEvents.APP_EVENT_UPDATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("appEventId", String.valueOf(createdAppEvent.getId()))
                .containsEntry("name", "order-placed");

            appEventFacade.delete(createdAppEvent.getId());

            assertThat(findSingleEvent(AppEventAuditEvents.APP_EVENT_DELETED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("appEventId", String.valueOf(createdAppEvent.getId()));
        }

        @Test
        void testDeleteOfAMissingAppEventIsNotRecorded() {
            assertThat(appEventFacade.delete(Long.MAX_VALUE)).isFalse();

            Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
                AuditEventFilter.empty(), PageRequest.of(0, 100));

            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested
    class IntegrationInstanceFacadeAudit {

        private static final long CONNECTED_USER_ID = 9301;

        @Test
        void testDeleteIntegrationInstanceRecordsTheInstanceId() {
            IntegrationInstance integrationInstance = createIntegrationInstance();

            integrationInstanceFacade.deleteIntegrationInstance(integrationInstance.getId());

            assertThat(integrationInstanceRepository.findById(integrationInstance.getId())).isEmpty();

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_DELETED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationInstanceId", String.valueOf(integrationInstance.getId()));
        }

        @Test
        void testDeleteOfAnUnknownIntegrationInstanceRecordsAFailure() {
            assertThatThrownBy(() -> integrationInstanceFacade.deleteIntegrationInstance(Long.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_DELETED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", IllegalArgumentException.class.getName())
                .containsEntry("integrationInstanceId", String.valueOf(Long.MAX_VALUE));
        }

        @Test
        void testEnableIntegrationInstanceRecordsTheNewState() {
            IntegrationInstance integrationInstance = createIntegrationInstance();

            ConnectedUser connectedUser = mock(ConnectedUser.class);

            when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);

            integrationInstanceFacade.enableIntegrationInstance(integrationInstance.getId(), true);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_ENABLED_UPDATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationInstanceId", String.valueOf(integrationInstance.getId()))
                .containsEntry("enabled", "true");
        }

        @Test
        void testEnableIntegrationInstanceWorkflowOfAnUnknownInstanceRecordsAFailure() {
            assertThatThrownBy(
                () -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(Long.MAX_VALUE, "workflow", false))
                    .isInstanceOf(RuntimeException.class);

            assertThat(
                findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_WORKFLOW_ENABLED_UPDATED).getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("integrationInstanceId", String.valueOf(Long.MAX_VALUE))
                    .containsEntry("workflowId", "workflow")
                    .containsEntry("enabled", "false");
        }

        @Test
        void testProvisionForAnUnknownConnectedUserRecordsTheRequestWithoutAnInstance() {
            when(connectedUserService.getConnectedUser(CONNECTED_USER_ID))
                .thenThrow(new NoSuchElementException("Connected user not found"));

            assertThatThrownBy(
                () -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                    CONNECTED_USER_ID, 7L, Map.of("token", "s3cr3t"), null))
                        .isInstanceOf(NoSuchElementException.class);

            assertThat(findSingleEvent(IntegrationAuditEvents.INTEGRATION_INSTANCE_PROVISIONED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("connectedUserId", String.valueOf(CONNECTED_USER_ID))
                .containsEntry("integrationInstanceConfigurationId", "7")
                .doesNotContainKey("integrationInstanceId")
                .doesNotContainValue("s3cr3t");
        }

        private IntegrationInstance createIntegrationInstance() {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                createIntegrationInstanceConfiguration();

            IntegrationInstance integrationInstance = integrationInstanceService.create(
                CONNECTED_USER_ID, 41L, integrationInstanceConfiguration.getId());

            persistenceAuditEventRepository.deleteAll();

            return integrationInstance;
        }
    }

    @Nested
    class IntegrationWorkflowFacadeAudit {

        @Test
        void testAddWorkflowRecordsTheIntegrationTheIntegrationWorkflowAndTheWorkflow() {
            Integration integration = createIntegration();

            String definition = "{\"label\": \"New Workflow\", \"tasks\": []}";

            String workflowId = String.valueOf(UUID.randomUUID());

            when(workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC))
                .thenReturn(new Workflow(workflowId, definition, Workflow.Format.JSON));

            long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(integration.getId(), definition);

            assertThat(integrationWorkflowRepository.findById(integrationWorkflowId)).isPresent();

            assertThat(findSingleEvent(WorkflowAuditEvents.INTEGRATION_WORKFLOW_CREATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("integrationId", String.valueOf(integration.getId()))
                .containsEntry("integrationWorkflowId", String.valueOf(integrationWorkflowId))
                .containsEntry("workflowId", workflowId);
        }

        @Test
        void testDeleteWorkflowRecordsTheWorkflowId() {
            Integration integration = createIntegration();

            String workflowId = String.valueOf(UUID.randomUUID());

            IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
                integration.getId(), integration.getLastIntegrationVersion(), workflowId);

            integrationWorkflowFacade.deleteWorkflow(workflowId);

            assertThat(integrationWorkflowRepository.findById(integrationWorkflow.getId())).isEmpty();

            assertThat(findSingleEvent(WorkflowAuditEvents.INTEGRATION_WORKFLOW_DELETED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("workflowId", workflowId);
        }

        @Test
        void testDeleteOfAnUnknownWorkflowRecordsAFailure() {
            String workflowId = String.valueOf(UUID.randomUUID());

            assertThatThrownBy(() -> integrationWorkflowFacade.deleteWorkflow(workflowId))
                .isInstanceOf(NoSuchElementException.class);

            assertThat(findSingleEvent(WorkflowAuditEvents.INTEGRATION_WORKFLOW_DELETED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("errorClass", NoSuchElementException.class.getName())
                .containsEntry("workflowId", workflowId);
        }

        @Test
        void testUpdatePermissionExpressionRecordsTheExpression() {
            Integration integration = createIntegration();

            IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
                integration.getId(), integration.getLastIntegrationVersion(), String.valueOf(UUID.randomUUID()));

            integrationWorkflowFacade.updatePermissionExpression(integrationWorkflow.getId(), PERMISSION_EXPRESSION);

            assertThat(integrationWorkflowService.getIntegrationWorkflow(integrationWorkflow.getId())
                .getPermissionExpression()).isEqualTo(PERMISSION_EXPRESSION);

            assertThat(
                findSingleEvent(WorkflowAuditEvents.INTEGRATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED).getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry("integrationWorkflowId", String.valueOf(integrationWorkflow.getId()))
                    .containsEntry("permissionExpression", PERMISSION_EXPRESSION);
        }

        @Test
        void testUpdatePermissionExpressionOfAnUnknownWorkflowRecordsAFailure() {
            assertThatThrownBy(
                () -> integrationWorkflowFacade.updatePermissionExpression(Long.MAX_VALUE, PERMISSION_EXPRESSION))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("IntegrationWorkflow not found for id: " + Long.MAX_VALUE);

            assertThat(
                findSingleEvent(WorkflowAuditEvents.INTEGRATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED).getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("errorClass", IllegalArgumentException.class.getName())
                    .containsEntry("integrationWorkflowId", String.valueOf(Long.MAX_VALUE))
                    .containsEntry("permissionExpression", PERMISSION_EXPRESSION);
        }
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
