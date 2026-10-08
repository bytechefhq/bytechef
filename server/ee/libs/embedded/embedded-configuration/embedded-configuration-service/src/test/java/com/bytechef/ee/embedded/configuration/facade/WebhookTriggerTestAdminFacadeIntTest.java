/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
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
    classes = {
        IntegrationIntTestConfiguration.class, WebhookTriggerTestAdminFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class WebhookTriggerTestAdminFacadeIntTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final String TRIGGER_NAME = "trigger_1";
    private static final String WORKFLOW_ID = "workflow-1";

    @MockitoBean
    private PermissionService permissionService;

    @Autowired
    private WebhookTriggerTestAdminFacade webhookTriggerTestAdminFacade;

    @Autowired
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @BeforeEach
    void beforeEach() {
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
    void testNonAdminIsDeniedBeforeTheSharedFacadeIsTouched() {
        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(
            () -> webhookTriggerTestAdminFacade.startWebhookTriggerTest(WORKFLOW_ID, TRIGGER_NAME, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> webhookTriggerTestAdminFacade.stopWebhookTriggerTest(WORKFLOW_ID, TRIGGER_NAME, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(webhookTriggerTestFacade);
    }

    @Test
    void testTenantAdminReachesTheSharedFacadeTypedEmbedded() {
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(webhookTriggerTestFacade.enableTrigger(WORKFLOW_ID, TRIGGER_NAME, ENVIRONMENT_ID, PlatformType.EMBEDDED))
            .thenReturn("https://example.org/webhook");

        assertThat(webhookTriggerTestAdminFacade.startWebhookTriggerTest(WORKFLOW_ID, TRIGGER_NAME, ENVIRONMENT_ID))
            .isEqualTo("https://example.org/webhook");

        webhookTriggerTestAdminFacade.stopWebhookTriggerTest(WORKFLOW_ID, TRIGGER_NAME, ENVIRONMENT_ID);

        verify(webhookTriggerTestFacade).disableTrigger(
            WORKFLOW_ID, TRIGGER_NAME, ENVIRONMENT_ID, PlatformType.EMBEDDED);
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            return new AutomationMethodSecurityExpressionHandler(permissionService);
        }
    }
}
