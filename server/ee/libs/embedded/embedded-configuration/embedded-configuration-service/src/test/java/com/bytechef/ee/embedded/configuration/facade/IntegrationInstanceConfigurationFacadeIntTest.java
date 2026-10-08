/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class,
        IntegrationInstanceConfigurationFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceConfigurationFacadeIntTest {

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @BeforeEach
    void beforeEach() {
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(new ConnectionDefinition(ComponentDsl.connection(), "componentName", null, null));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testListReadIsDeliberatelyOnlyAuthenticated() {
        long integrationInstanceConfigurationId = createEnabledIntegrationInstanceConfiguration();

        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThat(
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegrations(
                    true, Environment.PRODUCTION))
                        .as("list read allowed to %s", authentication.getName())
                        .extracting(IntegrationInstanceConfigurationDTO::id)
                        .contains(integrationInstanceConfigurationId);

            assertThatThrownBy(
                () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationId))
                        .as("by-id read denied to %s", authentication.getName())
                        .isInstanceOf(AccessDeniedException.class);
        }

        authenticate(anonymous());

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegrations(
                true, Environment.PRODUCTION))
                    .isInstanceOf(AccessDeniedException.class);
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication connectedUser() {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false);
    }

    private long createEnabledIntegrationInstanceConfiguration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        long integrationInstanceConfigurationId = integrationInstanceConfiguration.getId();

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, true);

        return integrationInstanceConfigurationId;
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
