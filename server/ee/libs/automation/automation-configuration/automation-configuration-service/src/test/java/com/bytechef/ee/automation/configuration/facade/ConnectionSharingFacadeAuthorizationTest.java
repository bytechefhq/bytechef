/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.connection.audit.AuditConnection;
import com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} expressions on the EE {@link WorkspaceConnectionFacadeImpl} sharing mutations.
 *
 * <p>
 * The guards live on the facade rather than on {@code ConnectionSharingGraphQlController} so they protect every caller
 * of the facade, not just the GraphQL entry point. This reflection test catches a refactor that silently drops one —
 * which would be invisible at runtime until someone exercised the unguarded path.
 *
 * <p>
 * Unlike the promote/demote surface this replaces, no mutation here wants the annotation <em>absent</em>:
 * owner-or-admin is expressible in SpEL, so there is no programmatic check to fall back to and no orphan-recovery hole
 * to keep open — an admin always satisfies the second disjunct.
 *
 * <p>
 * Runtime enforcement of the {@code @permissionService.<method>} bean-reference form is proven by
 * {@code PreAuthorizeProxyEnforcementIntTest}, which calls this very facade through its security proxy for both
 * disjuncts.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionSharingFacadeAuthorizationTest {

    private static final String OWNER_OR_ADMIN_EXPRESSION =
        "@permissionService.isResourceOwner('Connection', #connectionId) || " +
            "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')";

    @ParameterizedTest
    @ValueSource(strings = {
        "setConnectionVisibility", "grantConnectionAccess", "revokeConnectionAccess", "getConnectionGrants"
    })
    void testSharingMutationsRequireOwnerOrAdmin(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method '%s' must carry @PreAuthorize; dropping it would let any authenticated user change who can "
                    + "see a colleague's connection.",
                methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' must be owner-or-admin", methodName)
            .isEqualTo(OWNER_OR_ADMIN_EXPRESSION);
    }

    @ParameterizedTest
    @CsvSource({
        "setConnectionVisibility, CONNECTION_VISIBILITY_CHANGED",
        "grantConnectionAccess, CONNECTION_ACCESS_GRANTED",
        "revokeConnectionAccess, CONNECTION_ACCESS_REVOKED"
    })
    void testSharingMutationsAreAudited(String methodName, ConnectionAuditEvent expectedEvent) {
        AuditConnection auditConnection = findMethod(methodName).getAnnotation(AuditConnection.class);

        assertThat(auditConnection)
            .as(
                "Method '%s' must carry @AuditConnection; a change to who can see a credential that leaves no trail "
                    + "is a compliance blind spot.",
                methodName)
            .isNotNull();

        assertThat(auditConnection.event()).isEqualTo(expectedEvent);
    }

    @Test
    void testAccessRemovalIsStrictlyAudited() {
        // Losing access must not be able to happen untraceably, so an audit-capture failure fails the transaction
        // rather than being absorbed into a counter. Granting is not strict: a missing trail there over-reports
        // nobody's access.
        assertThat(ConnectionAuditEvent.CONNECTION_ACCESS_REVOKED.isStrictAudit()).isTrue();
        assertThat(ConnectionAuditEvent.CONNECTION_VISIBILITY_CHANGED.isStrictAudit()).isTrue();
        assertThat(ConnectionAuditEvent.CONNECTION_ACCESS_GRANTED.isStrictAudit()).isFalse();
    }

    @Test
    void testEverySharingMutationIsCovered() {
        List<String> annotatedMethods = Arrays.stream(WorkspaceConnectionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        // If a mutation is added to the facade without being listed in the @ValueSource above, this fails — so the
        // parameterized test cannot silently stop covering the full surface.
        assertThat(annotatedMethods)
            .containsExactly(
                "getConnectionGrants", "grantConnectionAccess", "revokeConnectionAccess", "setConnectionVisibility");
    }

    private static Method findMethod(String methodName) {
        return Arrays.stream(WorkspaceConnectionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getName()
                .equals(methodName))
            .filter(method -> !method.isSynthetic())
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "Expected exactly one non-synthetic '" + methodName
                        + "' method on the EE WorkspaceConnectionFacadeImpl"));
    }
}
