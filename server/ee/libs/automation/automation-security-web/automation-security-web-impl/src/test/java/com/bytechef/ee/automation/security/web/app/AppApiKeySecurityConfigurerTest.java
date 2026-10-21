/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppApiKeySecurityConfigurerTest {

    private static final String AUTOMATION_PATH_PATTERN = "^/api/automation/v[0-9]+/.+";

    @Test
    void testPathPatternMatchesAppApi() {
        Pattern pattern = Pattern.compile(AppApiKeySecurityConfigurer.PATH_PATTERN);

        assertThat(pattern.matcher("/api/app/v1/tools")
            .matches()).isTrue();
        assertThat(pattern.matcher("/api/app/v12/tools/X__Y/execute")
            .matches()).isTrue();
    }

    @Test
    void testPathPatternDoesNotMatchAutomationAppsManagementApi() {
        Pattern pattern = Pattern.compile(AppApiKeySecurityConfigurer.PATH_PATTERN);

        assertThat(pattern.matcher("/api/automation/v1/apps")
            .matches()).isFalse();
        assertThat(pattern.matcher("/api/automation/v1/apps/1/secret-key")
            .matches()).isFalse();
    }

    @Test
    void testPathPatternDoesNotMatchLookalikePrefixes() {
        Pattern pattern = Pattern.compile(AppApiKeySecurityConfigurer.PATH_PATTERN);

        assertThat(pattern.matcher("/api/application/v1/x")
            .matches()).isFalse();
        assertThat(pattern.matcher("/api/apps/v1/x")
            .matches()).isFalse();
        assertThat(pattern.matcher("/api/app/v1/")
            .matches()).isFalse();
        assertThat(pattern.matcher("/api/app/vx/tools")
            .matches()).isFalse();
        assertThat(pattern.matcher("/api/app")
            .matches()).isFalse();
    }

    @Test
    void testAutomationPathPatternDoesNotMatchAppApi() {
        Pattern pattern = Pattern.compile(AUTOMATION_PATH_PATTERN);

        assertThat(pattern.matcher("/api/app/v1/tools")
            .matches()).isFalse();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testRegisterCsrfOverrideIgnoresAppApi() {
        CsrfConfigurer csrf = mock(CsrfConfigurer.class);

        new AppApiKeySecurityConfigurer(mock(AppService.class), mock(EnvironmentService.class))
            .registerCsrfOverride(csrf);

        verify(csrf).ignoringRequestMatchers(any(RequestMatcher.class));
    }
}
