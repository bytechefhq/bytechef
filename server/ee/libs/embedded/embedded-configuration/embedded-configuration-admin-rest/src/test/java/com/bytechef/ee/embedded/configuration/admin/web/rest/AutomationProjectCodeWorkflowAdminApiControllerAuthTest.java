/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.admin.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.web.configurer.PlatformApiKeySecurityConfigurer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Pins the path -> security-configurer mapping that fixes the Critical auth finding: the CLI's
 * {@code embedded code-workflow deploy} command previously sent a plain platform API-key bearer token to
 * {@code /api/embedded/internal/automation/projects/deploy}, which is matched only by
 * {@code EmbeddedApiKeySecurityConfigurer}'s connected-user auth (requires a {@code /v<n>/{externalUserId}/} path
 * segment and grants zero authorities, so a bearer token can never satisfy the facade's {@code ROLE_ADMIN} guard).
 *
 * <p>
 * This controller instead mounts the same deploy operation under {@code /api/platform/v1/**}, mirroring the documented
 * {@code /api/platform/v1/custom-components/deploy} precedent ({@code CustomComponentApiController}): that surface is
 * matched by {@code PlatformApiKeySecurityConfigurer}, which authenticates the bearer token via {@code ApiKeyService}
 * and grants the underlying user's real Spring authorities, so an admin's platform API key reaches
 * {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save} with {@code ROLE_ADMIN} already granted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationProjectCodeWorkflowAdminApiControllerAuthTest {

    /**
     * Mirrors {@code EmbeddedApiKeySecurityConfigurer}'s connected-user matcher ({@code ^/api/embedded/v[0-9]+/.+}) --
     * the auth chain this fix moves the CLI command off of.
     */
    private static final Pattern EMBEDDED_CONNECTED_USER_PATTERN = Pattern.compile("^/api/embedded/v[0-9]+/.+");

    @Test
    void testAdminControllerIsMountedOnPlatformBasePath() {
        RequestMapping requestMapping = AutomationProjectCodeWorkflowAdminApiController.class.getAnnotation(
            RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("${openapi.openAPIDefinition.base-path.platform:}/v1");
    }

    @Test
    void testAdminDeployPathIsMatchedByPlatformApiKeySecurityConfigurer() throws Exception {
        String platformApiKeyPathPattern = readPlatformApiKeyPathPattern();

        // "${openapi.openAPIDefinition.base-path.platform:}" resolves to "/api/platform" per
        // openapi.openAPIDefinition.base-path.platform in application.yml.
        String resolvedPath =
            "/api/platform/v1" + AutomationProjectCodeWorkflowAdminApi.PATH_DEPLOY_AUTOMATION_PROJECT_CODE_WORKFLOW;

        assertThat(resolvedPath).matches(platformApiKeyPathPattern);
    }

    @Test
    void testAdminDeployPathIsNotMatchedByEmbeddedConnectedUserAuth() {
        String resolvedPath =
            "/api/platform/v1" + AutomationProjectCodeWorkflowAdminApi.PATH_DEPLOY_AUTOMATION_PROJECT_CODE_WORKFLOW;

        assertThat(EMBEDDED_CONNECTED_USER_PATTERN.matcher(resolvedPath)
            .matches()).isFalse();
    }

    @Test
    void testAdminListPathIsMatchedByPlatformApiKeySecurityConfigurer() throws Exception {
        String platformApiKeyPathPattern = readPlatformApiKeyPathPattern();

        String resolvedPath =
            "/api/platform/v1" + AutomationProjectCodeWorkflowAdminApi.PATH_LIST_AUTOMATION_PROJECT_CODE_WORKFLOWS;

        assertThat(resolvedPath).matches(platformApiKeyPathPattern);
    }

    @Test
    void testAdminListPathIsNotMatchedByEmbeddedConnectedUserAuth() {
        String resolvedPath =
            "/api/platform/v1" + AutomationProjectCodeWorkflowAdminApi.PATH_LIST_AUTOMATION_PROJECT_CODE_WORKFLOWS;

        assertThat(EMBEDDED_CONNECTED_USER_PATTERN.matcher(resolvedPath)
            .matches()).isFalse();
    }

    /**
     * Pins the Important-severity fix: {@code listAutomationProjectCodeWorkflows} reaches
     * {@code AutomationWorkflowProjectFacade#getPublishedProjects()}, which carries no {@code @PreAuthorize} because it
     * also backs the public connected-user catalog listing. The gate is therefore applied here, at the admin controller
     * method, mirroring the {@code hasAuthority(ADMIN)} guard on
     * {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save} for the sibling deploy endpoint. Behavioral 403/200
     * coverage lives in {@code AutomationProjectCodeWorkflowAdminApiControllerListAuthorizationIntTest}.
     */
    @Test
    void testListAutomationProjectCodeWorkflowsRequiresAdminAuthority() throws Exception {
        Method method = AutomationProjectCodeWorkflowAdminApiController.class.getMethod(
            "listAutomationProjectCodeWorkflows");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on listAutomationProjectCodeWorkflows")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")");
    }

    private static String readPlatformApiKeyPathPattern() throws Exception {
        Field field = PlatformApiKeySecurityConfigurer.class.getDeclaredField("PATH_PATTERN");

        field.setAccessible(true);

        return (String) field.get(null);
    }
}
