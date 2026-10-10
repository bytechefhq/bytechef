/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SpringJUnitConfig(PropertyCopilotFacadeTest.PropertyCopilotFacadeTestConfiguration.class)
class PropertyCopilotFacadeTest {

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private PropertyCopilotFacade propertyCopilotFacade;

    @Autowired
    private PropertyCopilotGenerator propertyCopilotGenerator;

    @BeforeEach
    void setUp() {
        reset(permissionEvaluator, propertyCopilotGenerator);

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user", "password", "ROLE_USER"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGenerateChecksWorkflowViewOnTheRequestedWorkflow() {
        givenPermission("wf1", true);

        when(propertyCopilotGenerator.generate(any())).thenReturn(PropertyCopilotResult.valid("Hello"));

        PropertyCopilotResult result = propertyCopilotFacade.generatePropertyValue(request("wf1"));

        assertThat(result.value()).isEqualTo("Hello");
        verify(permissionEvaluator).hasPermission(any(Authentication.class), eq("wf1"), eq("Workflow"),
            eq("WORKFLOW_VIEW"));
    }

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowView() {
        givenPermission("wf1", false);

        assertThatThrownBy(() -> propertyCopilotFacade.generatePropertyValue(request("wf1")))
            .isInstanceOf(AccessDeniedException.class);

        verify(propertyCopilotGenerator, never()).generate(any());
    }

    @Test
    void testGenerateDeniedForAnUnknownWorkflow() {
        givenPermission("unknown-workflow", false);

        assertThatThrownBy(() -> propertyCopilotFacade.generatePropertyValue(request("unknown-workflow")))
            .isInstanceOf(AccessDeniedException.class);

        verify(propertyCopilotGenerator, never()).generate(any());
    }

    @Test
    void testGenerateRejectedWhenGeneratorIsNotWired() {
        PropertyCopilotFacade facade = new PropertyCopilotFacadeImpl(Optional.empty());

        assertThatThrownBy(() -> facade.generatePropertyValue(request("wf1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Property Copilot is not enabled");
    }

    private void givenPermission(String workflowId, boolean granted) {
        when(permissionEvaluator.hasPermission(
            any(Authentication.class), eq(workflowId), eq("Workflow"), eq("WORKFLOW_VIEW")))
                .thenReturn(granted);
    }

    private static PropertyCopilotRequest request(String workflowId) {
        return new PropertyCopilotRequest(
            "greet", PropertyCopilotMode.TEXT, workflowId, "n1", "p", "STRING", true, 0);
    }

    @Configuration
    @EnableMethodSecurity
    static class PropertyCopilotFacadeTestConfiguration {

        @Bean
        PermissionEvaluator permissionEvaluator() {
            return mock(PermissionEvaluator.class);
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator) {

            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

            expressionHandler.setPermissionEvaluator(permissionEvaluator);

            return expressionHandler;
        }

        @Bean
        PropertyCopilotGenerator propertyCopilotGenerator() {
            return mock(PropertyCopilotGenerator.class);
        }

        @Bean
        PropertyCopilotFacade propertyCopilotFacade(PropertyCopilotGenerator propertyCopilotGenerator) {
            return new PropertyCopilotFacadeImpl(Optional.of(propertyCopilotGenerator));
        }
    }
}
