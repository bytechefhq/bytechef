/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close git-configuration IDOR (T25). Git config is a per-workspace
 * credential-bearing setting keyed by {@code workspaceId}; reading or saving it requires workspace admin. The internal
 * {@code getGitConfiguration(long)} used by the project git-sync listener self-invokes {@code fetchGitConfiguration}
 * (proxy-bypassed) and stays ungated so sync is unaffected -- a negative assertion locks that in.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class GitConfigurationFacadeAuthorizationTest {

    @Test
    void testFetchGitConfigurationRequiresWorkspaceAdmin() {
        assertExpression(
            "fetchGitConfiguration", "hasPermission(#workspaceId, 'WorkspaceRole', 'ADMIN')", long.class);
    }

    @Test
    void testSaveRequiresWorkspaceAdmin() {
        assertExpression(
            "save", "hasPermission(#workspaceId, 'WorkspaceRole', 'ADMIN')", GitConfigurationDTO.class, long.class);
    }

    @Test
    void testGetGitConfigurationIsNotGated() throws NoSuchMethodException {
        Method method = GitConfigurationFacadeImpl.class.getDeclaredMethod("getGitConfiguration", long.class);

        assertThat(method.isAnnotationPresent(PreAuthorize.class))
            .as("getGitConfiguration (used by the git-sync listener) must NOT carry @PreAuthorize")
            .isFalse();
    }

    private static void assertExpression(String methodName, String expression, Class<?>... parameterTypes) {
        Method method;

        try {
            method = GitConfigurationFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
