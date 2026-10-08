/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceJobPrincipalAuthenticationResolverTest {

    private static final long CONNECTED_USER_ID = 3L;
    private static final long INTEGRATION_INSTANCE_ID = 9L;

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);

    private final IntegrationInstanceJobPrincipalAuthenticationResolver resolver =
        new IntegrationInstanceJobPrincipalAuthenticationResolver(connectedUserService, integrationInstanceService);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetType() {
        assertThat(resolver.getType()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testFetchAuthenticationReturnsTheConnectedUserOfTheIntegrationInstance() {
        stubIntegrationInstance();
        stubConnectedUser(true);

        Authentication authentication = resolver.fetchAuthentication(INTEGRATION_INSTANCE_ID)
            .orElseThrow();

        assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);
        assertThat(authentication.getName()).isEqualTo("external-user");
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(((EmbeddedApiKeyAuthenticationToken) authentication).getEnvironmentId())
            .isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testFetchAuthenticationIsEmptyForADisabledConnectedUser() {
        stubIntegrationInstance();
        stubConnectedUser(false);

        assertThat(resolver.fetchAuthentication(INTEGRATION_INSTANCE_ID)).isEmpty();
    }

    @Test
    void testFetchAuthenticationIsEmptyForAMissingIntegrationInstance() {
        when(integrationInstanceService.getIntegrationInstances(List.of(INTEGRATION_INSTANCE_ID)))
            .thenReturn(List.of());

        assertThat(resolver.fetchAuthentication(INTEGRATION_INSTANCE_ID)).isEmpty();
    }

    @Test
    void testUnattendedRunActsAsTheConnectedUserUnderMethodSecurity() {
        stubIntegrationInstance();
        stubConnectedUser(true);

        GatedOperations gatedOperations = createGatedOperations();
        JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner = new JobPrincipalAuthenticationRunner(
            List.of(resolver));

        assertThat(
            SecurityUtils.runAsSystem(
                () -> jobPrincipalAuthenticationRunner.run(
                    PlatformType.EMBEDDED, INTEGRATION_INSTANCE_ID, "job 1",
                    gatedOperations::connectedUserOperation))).isEqualTo("granted");

        assertThatThrownBy(
            () -> SecurityUtils.runAsSystem(
                () -> jobPrincipalAuthenticationRunner.run(
                    PlatformType.EMBEDDED, INTEGRATION_INSTANCE_ID, "job 1",
                    () -> gatedOperations.resourceOwnerOperation(1L))))
                        .isInstanceOf(AccessDeniedException.class);
    }

    private void stubConnectedUser(boolean enabled) {
        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), null, enabled, "external-user", CONNECTED_USER_ID, null, 0);

        connectedUser.setEnvironment(Environment.PRODUCTION);

        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(connectedUser));
    }

    private void stubIntegrationInstance() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(CONNECTED_USER_ID);

        when(integrationInstanceService.getIntegrationInstances(List.of(INTEGRATION_INSTANCE_ID)))
            .thenReturn(List.of(integrationInstance));
    }

    private static GatedOperations createGatedOperations() {
        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(
            new AutomationMethodSecurityExpressionHandler(mock(PermissionService.class)));

        ProxyFactory proxyFactory = new ProxyFactory(new GatedOperations());

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (GatedOperations) proxyFactory.getProxy();
    }

    static class GatedOperations {

        @PreAuthorize("isTenantAdmin() or " +
            "T(com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications).isConnectedUser()")
        public String connectedUserOperation() {
            return "granted";
        }

        @PreAuthorize("isResourceOwner(#id, 'AiSkill')")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String resourceOwnerOperation(long id) {
            return "granted";
        }
    }
}
