/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppErrorTypeTest {

    @Test
    void testNameOfNamesEveryConstant() throws IllegalAccessException {
        int constantCount = 0;

        for (Field field : AppErrorType.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == AppErrorType.class) {
                AppErrorType errorType = (AppErrorType) field.get(null);

                assertThat(AppErrorType.nameOf(errorType.getErrorKey())).isEqualTo(field.getName());

                constantCount++;
            }
        }

        assertThat(constantCount).isEqualTo(53);
    }

    @Test
    void testOperationDevelopmentOnlyContinuesTheNumbering() {
        assertThat(AppErrorType.APP_OPERATION_DEVELOPMENT_ONLY.getErrorKey()).isEqualTo(132);
        assertThat(AppErrorType.nameOf(132)).isEqualTo("APP_OPERATION_DEVELOPMENT_ONLY");
    }

    @Test
    void testInstanceConfigurationErrorsContinueTheNumbering() {
        assertThat(AppErrorType.APP_INSTANCE_CONFIGURATION_INVALID.getErrorKey()).isEqualTo(133);
        assertThat(AppErrorType.nameOf(133)).isEqualTo("APP_INSTANCE_CONFIGURATION_INVALID");
        assertThat(AppErrorType.APP_INSTANCE_CONFIGURATION_TOO_LARGE.getErrorKey()).isEqualTo(134);
        assertThat(AppErrorType.nameOf(134)).isEqualTo("APP_INSTANCE_CONFIGURATION_TOO_LARGE");
    }

    @Test
    void testIconInvalidContinuesTheNumbering() {
        assertThat(AppErrorType.APP_ICON_INVALID.getErrorKey()).isEqualTo(135);
        assertThat(AppErrorType.nameOf(135)).isEqualTo("APP_ICON_INVALID");
    }

    @Test
    void testUserAuthUnavailableContinuesTheNumbering() {
        assertThat(AppErrorType.APP_USER_AUTH_UNAVAILABLE.getErrorKey()).isEqualTo(136);
        assertThat(AppErrorType.nameOf(136)).isEqualTo("APP_USER_AUTH_UNAVAILABLE");
    }

    @Test
    void testUserTokenErrorsContinueTheNumbering() {
        assertThat(AppErrorType.APP_USER_REQUIRED.getErrorKey()).isEqualTo(137);
        assertThat(AppErrorType.nameOf(137)).isEqualTo("APP_USER_REQUIRED");
        assertThat(AppErrorType.APP_USER_TOKEN_INVALID.getErrorKey()).isEqualTo(138);
        assertThat(AppErrorType.nameOf(138)).isEqualTo("APP_USER_TOKEN_INVALID");
        assertThat(AppErrorType.APP_USER_FORBIDDEN.getErrorKey()).isEqualTo(139);
        assertThat(AppErrorType.nameOf(139)).isEqualTo("APP_USER_FORBIDDEN");
    }

    @Test
    void testVersionAndDeploymentErrorsContinueTheNumbering() {
        assertThat(AppErrorType.APP_VERSION_NOT_FOUND.getErrorKey()).isEqualTo(140);
        assertThat(AppErrorType.nameOf(140)).isEqualTo("APP_VERSION_NOT_FOUND");
        assertThat(AppErrorType.APP_DEPLOYMENT_NOT_FOUND.getErrorKey()).isEqualTo(141);
        assertThat(AppErrorType.nameOf(141)).isEqualTo("APP_DEPLOYMENT_NOT_FOUND");
        assertThat(AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS.getErrorKey()).isEqualTo(142);
        assertThat(AppErrorType.nameOf(142)).isEqualTo("APP_VERSION_NAME_ALREADY_EXISTS");
        assertThat(AppErrorType.APP_VERSION_COMMIT_MISMATCH.getErrorKey()).isEqualTo(143);
        assertThat(AppErrorType.nameOf(143)).isEqualTo("APP_VERSION_COMMIT_MISMATCH");
        assertThat(AppErrorType.APP_DEPLOYMENT_TERMINAL.getErrorKey()).isEqualTo(144);
        assertThat(AppErrorType.nameOf(144)).isEqualTo("APP_DEPLOYMENT_TERMINAL");
        assertThat(AppErrorType.APP_DEPLOYMENT_URL_INVALID.getErrorKey()).isEqualTo(145);
        assertThat(AppErrorType.nameOf(145)).isEqualTo("APP_DEPLOYMENT_URL_INVALID");
        assertThat(AppErrorType.APP_LIST_LIMIT_INVALID.getErrorKey()).isEqualTo(146);
        assertThat(AppErrorType.nameOf(146)).isEqualTo("APP_LIST_LIMIT_INVALID");
    }

    @Test
    void testProviderDeploymentErrorsContinueTheNumbering() {
        assertThat(AppErrorType.APP_DEPLOYMENT_PROVIDER_UNAVAILABLE.getErrorKey()).isEqualTo(147);
        assertThat(AppErrorType.nameOf(147)).isEqualTo("APP_DEPLOYMENT_PROVIDER_UNAVAILABLE");
        assertThat(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS.getErrorKey()).isEqualTo(148);
        assertThat(AppErrorType.nameOf(148)).isEqualTo("APP_DEPLOYMENT_IN_PROGRESS");
        assertThat(AppErrorType.APP_DEPLOYMENT_PROVIDER_FAILED.getErrorKey()).isEqualTo(149);
        assertThat(AppErrorType.nameOf(149)).isEqualTo("APP_DEPLOYMENT_PROVIDER_FAILED");
    }

    @Test
    void testDeploymentSettingsInvalidContinuesTheNumbering() {
        assertThat(AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID.getErrorKey()).isEqualTo(150);
        assertThat(AppErrorType.nameOf(150)).isEqualTo("APP_DEPLOYMENT_SETTINGS_INVALID");
    }

    @Test
    void testCategoryInvalidContinuesTheNumbering() {
        assertThat(AppErrorType.APP_CATEGORY_INVALID.getErrorKey()).isEqualTo(151);
        assertThat(AppErrorType.nameOf(151)).isEqualTo("APP_CATEGORY_INVALID");
    }

    @Test
    void testInstanceUrlInvalidContinuesTheNumbering() {
        assertThat(AppErrorType.APP_INSTANCE_URL_INVALID.getErrorKey()).isEqualTo(152);
        assertThat(AppErrorType.nameOf(152)).isEqualTo("APP_INSTANCE_URL_INVALID");
    }

    @Test
    void testNameOfUnknownKeyIsNull() {
        assertThat(AppErrorType.nameOf(999)).isNull();
    }
}
