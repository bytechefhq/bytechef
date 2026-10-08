/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, IntegrationInstanceAdminFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceAdminFacadeIntTest {

    private static final long INTEGRATION_INSTANCE_ID = 1L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private IntegrationInstanceAdminFacade integrationInstanceAdminFacade;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private PermissionService permissionService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsDenied() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(
                INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(integrationInstanceFacade);
    }

    @Test
    void testNonAdminIsDenied() {
        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(
                INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(integrationInstanceFacade);
    }

    @Test
    void testTenantAdminReachesTheSharedFacade() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(permissionService.isTenantAdmin()).thenReturn(true);

        integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);

        verify(integrationInstanceFacade).enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
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
