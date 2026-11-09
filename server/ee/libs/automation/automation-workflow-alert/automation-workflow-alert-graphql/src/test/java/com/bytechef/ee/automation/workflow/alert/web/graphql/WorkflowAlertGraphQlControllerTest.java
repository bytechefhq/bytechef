/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.workflow.alert.dispatcher.WorkflowAlertDispatcher;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertEventService;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 * @author Ivica Cardic
 */
class WorkflowAlertGraphQlControllerTest {

    private AnnotationConfigApplicationContext applicationContext;
    private WorkflowAlertEventService workflowAlertEventService;
    private WorkflowAlertGraphQlController workflowAlertGraphQlController;
    private WorkflowAlertRuleService workflowAlertRuleService;

    @BeforeEach
    void beforeEach() {
        applicationContext = new AnnotationConfigApplicationContext(MethodSecurityTestConfiguration.class);

        workflowAlertEventService = applicationContext.getBean(WorkflowAlertEventService.class);
        workflowAlertGraphQlController = applicationContext.getBean(WorkflowAlertGraphQlController.class);
        workflowAlertRuleService = applicationContext.getBean(WorkflowAlertRuleService.class);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        applicationContext.close();
    }

    @Test
    void testWorkflowAlertRulesDeniesNonAdmin() {
        authenticate("ROLE_USER");

        assertThatThrownBy(() -> workflowAlertGraphQlController.workflowAlertRules(7L))
            .isInstanceOf(AccessDeniedException.class);

        verify(workflowAlertRuleService, never()).getWorkflowAlertRules(7L);
    }

    @Test
    void testWorkflowAlertRulesAllowsAdmin() {
        when(workflowAlertRuleService.getWorkflowAlertRules(7L)).thenReturn(List.of());

        authenticate("ROLE_ADMIN");

        assertThat(workflowAlertGraphQlController.workflowAlertRules(7L)).isEmpty();
    }

    @Test
    void testWorkflowAlertEventsDeniesNonAdmin() {
        authenticate("ROLE_USER");

        assertThatThrownBy(() -> workflowAlertGraphQlController.workflowAlertEvents(7L))
            .isInstanceOf(AccessDeniedException.class);

        verify(workflowAlertEventService, never()).getWorkflowAlertEvents(7L);
    }

    @Test
    void testWorkflowAlertEventsAllowsAdmin() {
        when(workflowAlertEventService.getWorkflowAlertEvents(7L)).thenReturn(List.of());

        authenticate("ROLE_ADMIN");

        assertThat(workflowAlertGraphQlController.workflowAlertEvents(7L)).isEmpty();
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "password", AuthorityUtils.createAuthorityList(authority)));
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {

        @Bean
        WorkflowAlertEventService workflowAlertEventService() {
            return mock(WorkflowAlertEventService.class);
        }

        @Bean
        WorkflowAlertRuleService workflowAlertRuleService() {
            return mock(WorkflowAlertRuleService.class);
        }

        @Bean
        WorkflowAlertGraphQlController workflowAlertGraphQlController(
            WorkflowAlertEventService workflowAlertEventService, WorkflowAlertRuleService workflowAlertRuleService) {

            return new WorkflowAlertGraphQlController(
                mock(WorkflowAlertDispatcher.class), workflowAlertEventService, workflowAlertRuleService);
        }
    }
}
