/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationWorkflowExecutionFacadeIntTest.Config.class,
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    ComponentDefinitionService.class, ContextService.class, EnvironmentService.class,
    Evaluator.class, IntegrationInstanceConfigurationService.class, IntegrationInstanceService.class,
    IntegrationService.class, IntegrationWorkflowFacade.class, IntegrationWorkflowService.class,
    TaskDispatcherDefinitionService.class, TaskFileStorage.class, TriggerExecutionService.class,
    TriggerFileStorage.class, WorkflowService.class
})
class IntegrationWorkflowExecutionFacadeIntTest {

    @Autowired
    private IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @MockitoBean
    private PrincipalJobService principalJobService;

    @MockitoBean
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testNonAdminIsDeniedEveryAdminReadBeforeAnyLookup() {
        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(1L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(1L, 2L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
                null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(jobService, principalJobService, taskExecutionService);
    }

    @Test
    void testTenantAdminPassesTheGate() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(1L))
            .isNotInstanceOf(AccessDeniedException.class);

        verify(jobService).getJob(1L);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(IntegrationWorkflowExecutionFacadeImpl.class)
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
