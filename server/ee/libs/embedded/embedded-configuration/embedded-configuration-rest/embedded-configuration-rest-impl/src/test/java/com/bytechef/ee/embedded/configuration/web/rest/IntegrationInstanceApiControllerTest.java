/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.web.rest.TenantAdminGateTestSupport.GateRecorder;
import com.bytechef.ee.embedded.configuration.web.rest.TenantAdminGateTestSupport.TenantAdminExpressionHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationInstanceApiControllerTest.Config.class)
class IntegrationInstanceApiControllerTest {

    private static final String ADMIN_EXPRESSION = "isTenantAdmin()";
    private static final long INTEGRATION_INSTANCE_ID = 1L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private IntegrationInstanceApiController controller;

    @Autowired
    private GateRecorder gateRecorder;

    @Autowired
    private IntegrationInstanceFacade integrationInstanceFacade;

    @BeforeEach
    void setUp() {
        reset(integrationInstanceFacade);

        gateRecorder.reset();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesCallerWhoIsNotTenantAdmin() {
        gateRecorder.permit(false);

        assertThatThrownBy(
            () -> controller.enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verifyNoInteractions(integrationInstanceFacade);
    }

    @Test
    void testEnableIntegrationInstanceWorkflowPermitsTenantAdmin() {
        gateRecorder.permit(true);

        controller.enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verify(integrationInstanceFacade).enableIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);
    }

    @Test
    void testEnableIntegrationInstanceWorkflowRequiresTenantAdmin() {
        List<Method> methods = Arrays.stream(IntegrationInstanceApiController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals("enableIntegrationInstanceWorkflow"))
            .toList();

        assertThat(methods)
            .as("Expected exactly one non-synthetic 'enableIntegrationInstanceWorkflow' on the controller")
            .hasSize(1);

        PreAuthorize preAuthorize = methods.get(0)
            .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "@PreAuthorize on enableIntegrationInstanceWorkflow. This /internal operation serves the admin "
                    + "console only; connected users toggle their own instances through the /v1 API.")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(ADMIN_EXPRESSION);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {

        @Bean
        ConversionService conversionService() {
            return mock(ConversionService.class);
        }

        @Bean
        GateRecorder gateRecorder() {
            return new GateRecorder();
        }

        @Bean
        IntegrationInstanceApiController integrationInstanceApiController(
            ConversionService conversionService, IntegrationInstanceFacade integrationInstanceFacade) {

            return new IntegrationInstanceApiController(conversionService, integrationInstanceFacade);
        }

        @Bean
        IntegrationInstanceFacade integrationInstanceFacade() {
            return mock(IntegrationInstanceFacade.class);
        }

        @Bean
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(GateRecorder gateRecorder) {
            return new TenantAdminExpressionHandler(gateRecorder);
        }
    }
}
