/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.internal.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.platform.security.web.configurer.PlatformApiKeySecurityConfigurer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * Pins the fix for the broken admin-console custom-component upload: the client posted cookie/XSRF only (no
 * {@code Authorization} header) to {@code /api/platform/v1/custom-components/deploy}, which is matched unconditionally
 * by {@code PlatformApiKeySecurityConfigurer.PATH_PATTERN} -- its converter throws {@code BadCredentialsException} when
 * no {@code Authorization} header is present, so the request was rejected with 401 before
 * {@code CustomComponentApiController} ever ran.
 *
 * <p>
 * This controller instead mounts the same deploy operation under {@code /api/platform/internal/**}, which
 * {@code PlatformApiKeySecurityConfigurer.PATH_PATTERN} (({@code ^/api/platform/v[0-9]+/.+})) does not match. The
 * request therefore never enters the platform API-key filter and falls straight through to the base {@code /api/**}
 * chain's ordinary session-based authentication (see {@code SecurityConfiguration.apiFilterChain}), exactly the
 * cookie+XSRF mechanism the admin-console browser client already uses.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentInternalApiControllerAuthTest {

    /**
     * Mirrors {@code EmbeddedApiKeySecurityConfigurer}'s {@code /internal} matcher shape
     * ({@code ^/api/(?:automation|embedded|platform)/internal/.+}), which is ANDed with
     * {@code request.getHeader("Authorization") != null}. A cookie-only request (no {@code Authorization} header) never
     * satisfies that AND, so even though the internal path shape matches, the request is not intercepted by that filter
     * either and reaches ordinary session authentication.
     */
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

        // "${openapi.openAPIDefinition.base-path.platform:}" resolves to "/api/platform" per
        // openapi.openAPIDefinition.base-path.platform in application.yml.
        String resolvedPath =
            "/api/platform/internal" + CustomComponentInternalApi.PATH_DEPLOY_CUSTOM_COMPONENT_INTERNAL;

        assertThat(resolvedPath).doesNotMatch(platformApiKeyPathPattern);
    }

    @Test
    void testPublicDeployPathIsStillMatchedByPlatformApiKeySecurityConfigurer() throws Exception {
        String platformApiKeyPathPattern = readPlatformApiKeyPathPattern();

        // The bearer-token surface for CLI/curl callers must remain reachable through the API-key filter.
        String resolvedPath = "/api/platform/v1/custom-components/deploy";

        assertThat(resolvedPath).matches(platformApiKeyPathPattern);
    }

    @Test
    void testInternalDeployPathFallsThroughEmbeddedApiKeyMatcherWithoutAuthorizationHeader() {
        String resolvedPath =
            "/api/platform/internal" + CustomComponentInternalApi.PATH_DEPLOY_CUSTOM_COMPONENT_INTERNAL;

        // The path shape matches, but without an Authorization header the ANDed condition in
        // EmbeddedApiKeySecurityConfigurer is false, so a cookie-only request is not intercepted there either.
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

    /**
     * Closes the authorization chain this class otherwise only half-proves. The routing tests above show the internal
     * path is reachable by a session (cookie) caller; {@code CustomComponentFacadeAuthorizationTest} separately pins
     * {@code hasAuthority("ROLE_ADMIN")} on {@code CustomComponentFacadeImpl#save}. Neither means anything unless this
     * controller actually routes to that gated method -- if it ever called an ungated sibling, both tests would stay
     * green while the endpoint became open to any authenticated user.
     */
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
