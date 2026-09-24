/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedConnectedUserMethodSecurityTest {

    private GatedOperations gatedOperations;

    @BeforeEach
    void setUp() {
        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(
            new AutomationMethodSecurityExpressionHandler(mock(PermissionService.class)));

        ProxyFactory proxyFactory = new ProxyFactory(new GatedOperations());

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(
            preAuthorizeAuthorizationManager));

        gatedOperations = (GatedOperations) proxyFactory.getProxy();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsDeniedTenantAdminOperationUnderSkipChecks() {
        authenticate(createConnectedUserAuthentication());

        assertThatThrownBy(() -> callSkippingChecks(gatedOperations::tenantAdminOperation))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testConnectedUserIsDeniedResourceOwnerOperationUnderSkipChecks() {
        authenticate(createConnectedUserAuthentication());

        assertThatThrownBy(() -> callSkippingChecks(() -> gatedOperations.resourceOwnerOperation(1L)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testConnectedUserIsDeniedCurrentUserOperationUnderSkipChecks() {
        authenticate(createConnectedUserAuthentication());

        assertThatThrownBy(() -> callSkippingChecks(() -> gatedOperations.currentUserOperation(1L)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testConnectedUserIsGrantedConnectedUserOperation() throws Throwable {
        authenticate(createConnectedUserAuthentication());

        assertThat(gatedOperations.connectedUserOperation()).isEqualTo("granted");
        assertThat(callSkippingChecks(gatedOperations::connectedUserOperation)).isEqualTo("granted");
    }

    @Test
    void testApiKeyPrincipalKeepsSkipChecksBypass() throws Throwable {
        authenticate(new PlatformUserApiKeyAuthenticationToken(
            new User("user@localhost.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER")))));

        assertThat(callSkippingChecks(gatedOperations::tenantAdminOperation)).isEqualTo("granted");
        assertThat(callSkippingChecks(() -> gatedOperations.resourceOwnerOperation(1L))).isEqualTo("granted");
        assertThat(callSkippingChecks(() -> gatedOperations.currentUserOperation(1L))).isEqualTo("granted");
    }

    @Test
    void testMissingAuthenticationKeepsSkipChecksBypass() throws Throwable {
        assertThat(callSkippingChecks(gatedOperations::tenantAdminOperation)).isEqualTo("granted");
        assertThat(callSkippingChecks(() -> gatedOperations.resourceOwnerOperation(1L))).isEqualTo("granted");
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static <T> T callSkippingChecks(Supplier<T> supplier) throws Throwable {
        return AutomationAuthorizationContext.callSkippingChecks(supplier::get);
    }

    private static EmbeddedApiKeyAuthenticationToken createConnectedUserAuthentication() {
        return new EmbeddedApiKeyAuthenticationToken(1L, new User("external-user", "", List.of()));
    }

    static class GatedOperations {

        @PreAuthorize("isTenantAdmin()")
        public String tenantAdminOperation() {
            return "granted";
        }

        @PreAuthorize("isResourceOwner(#id, 'SigningKey')")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String resourceOwnerOperation(long id) {
            return "granted";
        }

        @PreAuthorize("isTenantAdmin() or isCurrentUser(#id)")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String currentUserOperation(long id) {
            return "granted";
        }

        @PreAuthorize("isTenantAdmin() or isConnectedUser()")
        public String connectedUserOperation() {
            return "granted";
        }
    }

    private static final class PlatformUserApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private PlatformUserApiKeyAuthenticationToken(User user) {
            super(1L, user);
        }
    }
}
