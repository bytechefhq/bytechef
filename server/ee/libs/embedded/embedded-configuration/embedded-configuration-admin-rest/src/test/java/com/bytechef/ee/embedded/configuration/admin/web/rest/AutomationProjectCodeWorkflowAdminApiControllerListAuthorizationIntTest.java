/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.admin.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.admin.web.rest.model.AutomationWorkflowProjectModel;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Behavioral coverage for the
 * {@link AutomationProjectCodeWorkflowAdminApiController#listAutomationProjectCodeWorkflows()} {@code @PreAuthorize}
 * gate added to close the Important-severity finding: the endpoint used to reach
 * {@code AutomationWorkflowProjectFacade#getPublishedProjects()} -- which carries no authorization of its own, because
 * it also serves the public connected-user catalog listing -- with no gate at all, so any non-admin tenant API key
 * could enumerate the catalog. The proxy is exercised directly (bean method call under {@code @EnableMethodSecurity}),
 * mirroring {@code PreAuthorizeProxyEnforcementIntTest}, rather than through the full HTTP filter chain.
 *
 * <p>
 * The bean is autowired by its {@code AutomationProjectCodeWorkflowAdminApi} interface, not the concrete controller
 * class -- {@code @EnableMethodSecurity} proxies interface-implementing beans with a JDK dynamic proxy by default,
 * which does not implement the concrete class.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationProjectCodeWorkflowAdminApiControllerListAuthorizationIntTest.Config.class)
class AutomationProjectCodeWorkflowAdminApiControllerListAuthorizationIntTest {

    @Autowired
    private AutomationProjectCodeWorkflowAdminApi controller;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testListAutomationProjectCodeWorkflowsDeniedForNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "non-admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(controller::listAutomationProjectCodeWorkflows)
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testListAutomationProjectCodeWorkflowsAllowedForAdmin() {
        when(automationWorkflowProjectFacade.getPublishedProjects())
            .thenReturn(List.of(
                new AutomationWorkflowProjectDTO(
                    1L, "project-1", null, null, List.of(), true, 1, 1, List.of(), null, false)));

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        ResponseEntity<List<AutomationWorkflowProjectModel>> response =
            controller.listAutomationProjectCodeWorkflows();

        assertThat(response.getStatusCode()
            .is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    static class Config {

        @Bean
        AutomationProjectCodeWorkflowAdminApiController automationProjectCodeWorkflowAdminApiController(
            AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade,
            AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {

            return new AutomationProjectCodeWorkflowAdminApiController(
                automationWorkflowProjectCodeWorkflowFacade, automationWorkflowProjectFacade);
        }

        @Bean
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade() {
            return mock(AutomationWorkflowProjectCodeWorkflowFacade.class);
        }

        @Bean
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade() {
            return mock(AutomationWorkflowProjectFacade.class);
        }
    }
}
