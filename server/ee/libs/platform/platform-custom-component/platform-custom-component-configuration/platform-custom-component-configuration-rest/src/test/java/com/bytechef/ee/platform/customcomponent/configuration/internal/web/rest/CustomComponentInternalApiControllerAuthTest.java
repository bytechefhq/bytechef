/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.internal.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.ee.platform.security.web.configurer.PlatformApiKeySecurityConfigurer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentInternalApiControllerAuthTest {

    private static final Pattern EMBEDDED_INTERNAL_PATH_SHAPE =
        Pattern.compile("^/api/(?:automation|embedded|platform)/internal/.+");

    @Test
    void testControllerIsMountedOnPlatformInternalBasePath() {
        RequestMapping requestMapping = CustomComponentInternalApiController.class.getAnnotation(
            RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("${openapi.openAPIDefinition.base-path.platform:}/internal");
    }

    @Test
    void testInternalDeployPathIsNotMatchedByPlatformApiKeySecurityConfigurer() throws Exception {
        String platformApiKeyPathPattern = readPlatformApiKeyPathPattern();

        String resolvedPath =
            "/api/platform/internal" + CustomComponentInternalApi.PATH_DEPLOY_CUSTOM_COMPONENT_INTERNAL;

        assertThat(resolvedPath).doesNotMatch(platformApiKeyPathPattern);
    }

    @Test
    void testPublicDeployPathIsStillMatchedByPlatformApiKeySecurityConfigurer() throws Exception {
        String platformApiKeyPathPattern = readPlatformApiKeyPathPattern();

        String resolvedPath = "/api/platform/v1/custom-components/deploy";

        assertThat(resolvedPath).matches(platformApiKeyPathPattern);
    }

    @Test
    void testInternalDeployPathFallsThroughEmbeddedApiKeyMatcherWithoutAuthorizationHeader() {
        String resolvedPath =
            "/api/platform/internal" + CustomComponentInternalApi.PATH_DEPLOY_CUSTOM_COMPONENT_INTERNAL;

        assertThat(EMBEDDED_INTERNAL_PATH_SHAPE.matcher(resolvedPath)
            .matches()).isTrue();

        MockHttpServletRequest cookieOnlyRequest = new MockHttpServletRequest();

        cookieOnlyRequest.setRequestURI(resolvedPath);

        boolean interceptedWithoutAuthorizationHeader = EMBEDDED_INTERNAL_PATH_SHAPE.matcher(resolvedPath)
            .matches() && cookieOnlyRequest.getHeader("Authorization") != null;

        assertThat(interceptedWithoutAuthorizationHeader).isFalse();
    }

    private static String readPlatformApiKeyPathPattern() throws Exception {
        Field field = PlatformApiKeySecurityConfigurer.class.getDeclaredField("PATH_PATTERN");

        field.setAccessible(true);

        return (String) field.get(null);
    }

    @Test
    void testDeployRoutesToTheAdminGatedFacadeSaveMethod() throws Exception {
        Method method = CustomComponentInternalApiController.class.getDeclaredMethod(
            "deployCustomComponentInternal", MultipartFile.class);

        assertThat(method).isNotNull();

        boolean callsGatedSave = Arrays.stream(CustomComponentFacade.class.getDeclaredMethods())
            .anyMatch(facadeMethod -> "save".equals(facadeMethod.getName()));

        assertThat(callsGatedSave)
            .as("CustomComponentFacade must still declare the ADMIN-gated save(...) this controller delegates to")
            .isTrue();
    }
}
