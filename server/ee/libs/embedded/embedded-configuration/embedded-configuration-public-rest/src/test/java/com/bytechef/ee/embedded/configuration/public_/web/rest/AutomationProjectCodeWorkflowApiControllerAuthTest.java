/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.constant.AuthorityConstants;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Pins the half of the path -> security-configurer mapping that this module owns.
 *
 * <p>
 * The operations are mounted on the embedded base path but must not be authenticated by
 * {@code EmbeddedApiKeySecurityConfigurer}'s connected-user auth, which requires a {@code /v<n>/{externalUserId}/} path
 * segment and grants zero authorities -- a bearer token can never satisfy the facade's {@code ROLE_ADMIN} guard through
 * it. {@code EmbeddedPlatformUserApiKeySecurityConfigurer} carves them out instead; that carve-out is pinned from the
 * other side by {@code EmbeddedPlatformUserApiKeyPathRoutingTest}, which matches on the literal path prefix asserted
 * here. Changing the prefix in {@code openapi.yaml} without widening that configurer's {@code PATH_PATTERN} fails this
 * test rather than silently falling back to connected-user auth.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationProjectCodeWorkflowApiControllerAuthTest {

    /**
     * The prefix {@code EmbeddedPlatformUserApiKeySecurityConfigurer.PATH_PATTERN} claims.
     */
    private static final Pattern CARVED_OUT_PREFIX_PATTERN = Pattern.compile("^/automation-project-code-workflows.*");

    @Test
    void testControllerIsMountedOnEmbeddedBasePath() {
        RequestMapping requestMapping = AutomationProjectCodeWorkflowApiController.class.getAnnotation(
            RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("${openapi.openAPIDefinition.base-path.embedded:}/v1");
    }

    @Test
    void testDeployPathCarriesTheCarvedOutPrefix() {
        assertThat(CARVED_OUT_PREFIX_PATTERN
            .matcher(AutomationProjectCodeWorkflowApi.PATH_DEPLOY_AUTOMATION_PROJECT_CODE_WORKFLOW)
            .matches()).isTrue();
    }

    @Test
    void testListPathCarriesTheCarvedOutPrefix() {
        assertThat(CARVED_OUT_PREFIX_PATTERN
            .matcher(AutomationProjectCodeWorkflowApi.PATH_LIST_AUTOMATION_PROJECT_CODE_WORKFLOWS)
            .matches()).isTrue();
    }

    /**
     * {@code listAutomationProjectCodeWorkflows} reaches
     * {@code AutomationWorkflowProjectFacade#getPublishedProjects()}, which carries no {@code @PreAuthorize} because it
     * also backs the public connected-user catalog listing. The gate is therefore applied at the controller method,
     * mirroring the {@code hasAuthority(ADMIN)} guard on {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save}
     * for the sibling deploy endpoint. Behavioral 403/200 coverage lives in
     * {@code AutomationProjectCodeWorkflowApiControllerListAuthorizationIntTest}.
     */
    @Test
    void testListAutomationProjectCodeWorkflowsRequiresAdminAuthority() throws Exception {
        Method method = AutomationProjectCodeWorkflowApiController.class.getMethod(
            "listAutomationProjectCodeWorkflows");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on listAutomationProjectCodeWorkflows")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")");
    }
}
