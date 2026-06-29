/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions gating project Git operations (T22): pull/branches require
 * DEPLOYMENT_PULL, push requires DEPLOYMENT_PUSH, all keyed on the owning project via {@code ProjectScope}.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ProjectGitFacadeAuthorizationTest {

    @Test
    void testPullRequiresDeploymentPull() {
        assertExpression("pullProjectFromGit", "hasPermission(#projectId, 'Project', 'DEPLOYMENT_PULL')");
    }

    @Test
    void testGetRemoteBranchesRequiresDeploymentPull() {
        assertExpression("getRemoteBranches", "hasPermission(#projectId, 'Project', 'DEPLOYMENT_PULL')");
    }

    @Test
    void testPushRequiresDeploymentPush() {
        assertExpression("pushProjectToGit", "hasPermission(#projectId, 'Project', 'DEPLOYMENT_PUSH')");
    }

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : ProjectGitFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
