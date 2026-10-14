/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedPlatformUserApiKeySecurityConfigurerTest {

    private static final Pattern PATH_PATTERN =
        Pattern.compile(EmbeddedPlatformUserApiKeySecurityConfigurer.PATH_PATTERN);

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/embedded/v1/automation-project-code-workflows",
        "/api/embedded/v1/automation-project-code-workflows/deploy",
        "/api/embedded/v2/automation-project-code-workflows",
        "/api/embedded/v1/integration-code-workflows/deploy"
    })
    void testClaimsPlatformUserPaths(String path) {
        assertThat(PATH_PATTERN.matcher(path)
            .matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/embedded/v1/user-1/integrations",
        "/api/embedded/v1/integrations",
        "/api/embedded/v1/app-events",
        "/api/embedded/v1/user-1/app-events",
        "/api/embedded/v1/workflows/some-uuid",
        "/api/embedded/v1/automation/projects",
        "/api/embedded/v1/automation-project-code-workflows-extra",
        "/api/embedded/internal/integrations/deploy"
    })
    void testLeavesConnectedUserPathsAlone(String path) {
        assertThat(PATH_PATTERN.matcher(path)
            .matches()).isFalse();
    }
}
