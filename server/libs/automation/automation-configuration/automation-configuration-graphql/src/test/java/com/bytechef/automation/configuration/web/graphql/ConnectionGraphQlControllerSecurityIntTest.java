/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * End-to-end Spring Security harness complementing {@link ConnectionGraphQlControllerAuthorizationTest}. That class
 * pins the presence / absence of {@code @PreAuthorize} annotations via reflection so a refactor cannot silently drop
 * them; this class additionally proves that when the annotations ARE present, Spring Security actually enforces them.
 *
 * <p>
 * The harness is deliberately lightweight: it builds a Spring-AOP proxy around the controller with an
 * {@link AuthorizationManagerBeforeMethodInterceptor} bound to {@code @PreAuthorize} — the same interceptor
 * {@code @EnableMethodSecurity} registers in production. Using the proxy directly (instead of {@code @SpringBootTest})
 * keeps the test hermetic and the assertion clearly scoped to "Spring Security's interceptor rejects on missing
 * authority". A full Spring Boot test would add minutes to the suite without strengthening this particular claim.
 *
 * @author Ivica Cardic
 */
class ConnectionGraphQlControllerSecurityIntTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long CONNECTION_ID = 42L;

    @AfterEach
    void clearSecurityContext() {
        // Pooled worker threads — if a test forgot to clear, the next test would inherit authority.
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPromoteConnectionToWorkspaceDeniedForNonAdmin() {
        authenticateAs("ROLE_USER");

        ConnectionGraphQlController proxy = createSecuredProxy(Mockito.mock(WorkspaceConnectionFacade.class));

        assertThatThrownBy(() -> proxy.promoteConnectionToWorkspace(WORKSPACE_ID, CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testPromoteConnectionToWorkspaceAllowedForAdmin() {
        authenticateAs("ROLE_ADMIN");

        WorkspaceConnectionFacade facade = Mockito.mock(WorkspaceConnectionFacade.class);
        ConnectionGraphQlController proxy = createSecuredProxy(facade);

        assertDoesNotThrow(() -> proxy.promoteConnectionToWorkspace(WORKSPACE_ID, CONNECTION_ID));

        Mockito.verify(facade)
            .promoteToWorkspace(WORKSPACE_ID, CONNECTION_ID);
    }

    @Test
    void testDisconnectConnectionDeniedForNonAdmin() {
        authenticateAs("ROLE_USER");

        ConnectionGraphQlController proxy = createSecuredProxy(Mockito.mock(WorkspaceConnectionFacade.class));

        assertThatThrownBy(() -> proxy.disconnectConnection(CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testPromoteAllPrivateConnectionsToWorkspaceDeniedForNonAdmin() {
        authenticateAs("ROLE_USER");

        ConnectionGraphQlController proxy = createSecuredProxy(Mockito.mock(WorkspaceConnectionFacade.class));

        assertThatThrownBy(() -> proxy.promoteAllPrivateConnectionsToWorkspace(WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testSetConnectionProjectsDeniedForNonAdmin() {
        authenticateAs("ROLE_USER");

        ConnectionGraphQlController proxy = createSecuredProxy(Mockito.mock(WorkspaceConnectionFacade.class));

        assertThatThrownBy(() -> proxy.setConnectionProjects(WORKSPACE_ID, CONNECTION_ID, List.of()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testShareConnectionToProjectDeniedForNonAdmin() {
        authenticateAs("ROLE_USER");

        ConnectionGraphQlController proxy = createSecuredProxy(Mockito.mock(WorkspaceConnectionFacade.class));

        assertThatThrownBy(() -> proxy.shareConnectionToProject(WORKSPACE_ID, CONNECTION_ID, 100L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRevokeConnectionFromProjectDeniedForNonAdmin() {
        authenticateAs("ROLE_USER");

        ConnectionGraphQlController proxy = createSecuredProxy(Mockito.mock(WorkspaceConnectionFacade.class));

        assertThatThrownBy(() -> proxy.revokeConnectionFromProject(WORKSPACE_ID, CONNECTION_ID, 100L))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * {@code demoteConnectionToPrivate} has NO {@code @PreAuthorize}, so a non-admin call must reach the facade (where
     * the admin-OR-creator check lives). This complements
     * {@link ConnectionGraphQlControllerAuthorizationTest#testDemoteConnectionToPrivateIsNotAnnotatedSoFacadeCanDoAdminOrCreatorCheck()}
     * — that reflection test asserts the annotation is absent; this test asserts the consequence at runtime (the method
     * is reachable by non-admin callers so orphan-recovery still works).
     */
    @Test
    void testDemoteConnectionToPrivateReachesFacadeForNonAdmin() {
        authenticateAs("ROLE_USER");

        WorkspaceConnectionFacade facade = Mockito.mock(WorkspaceConnectionFacade.class);
        ConnectionGraphQlController proxy = createSecuredProxy(facade);

        // Whether the facade then allows or rejects the call is the facade's concern — tested in
        // WorkspaceConnectionFacadeTest. What matters here is that Spring Security did not short-circuit
        // before the facade got a chance to run its admin-OR-creator check.
        proxy.demoteConnectionToPrivate(WORKSPACE_ID, CONNECTION_ID);

        Mockito.verify(facade)
            .demoteToPrivate(WORKSPACE_ID, CONNECTION_ID);
    }

    private static void authenticateAs(String authority) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "test-user", "n/a", List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(context);
    }

    private static ConnectionGraphQlController createSecuredProxy(WorkspaceConnectionFacade facade) {
        ConnectionGraphQlController target = new ConnectionGraphQlController(facade);

        ProxyFactory factory = new ProxyFactory(target);

        factory.setProxyTargetClass(true);
        factory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());

        return (ConnectionGraphQlController) factory.getProxy();
    }
}
