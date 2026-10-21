/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.audit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.automation.app.util.AppSecretKeys;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppAuditDataSecretsTest {

    @Test
    void testDataWithoutSecretsPasses() {
        AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_UPDATED, 1L,
            Map.of("appInstanceId", 2L, "changedFields", List.of("configuration", "name"), "name", "Production"));
        appAuditPublisher.publish(AppAuditEvent.APP_SUSPENDED, 1L);

        assertThatCode(() -> AppAuditDataAssertions.assertNoSecrets(appAuditPublisher, "prj_1"))
            .doesNotThrowAnyException();
    }

    @Test
    void testSecretKeyNamesAreRejected() {
        for (String key : List.of(
            "secretKey", "secretKeyHint", "keyHash", "userToken", "accessToken", "connectionParameters",
            "authorizationParameters", "deploymentSettings", "configuration", "password", "credentials")) {

            AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

            appAuditPublisher.publish(AppAuditEvent.APP_INSTANCE_UPDATED, 1L, Map.of(key, "x"));

            assertThatThrownBy(() -> AppAuditDataAssertions.assertNoSecrets(appAuditPublisher))
                .as(key)
                .isInstanceOf(AssertionError.class);
        }
    }

    @Test
    void testForbiddenValuesAreRejectedAtAnyDepth() {
        AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_UPDATED, 1L, Map.of("detail", Map.of("values", List.of("prj_1"))));

        assertThatThrownBy(() -> AppAuditDataAssertions.assertNoSecrets(appAuditPublisher, "prj_1"))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testARawAppKeyUnderAHarmlessFieldNameIsRejectedWithoutCallerInput() {
        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        assertRejected(Map.of("note", generatedKey.key()));
        assertRejected(Map.of("note", generatedKey.hash()));
        assertRejected(Map.of("details", List.of(Map.of("inner", Map.of("label", generatedKey.key())))));
    }

    @Test
    void testAJwtUnderAHarmlessFieldNameIsRejectedWithoutCallerInput() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJtZW1iZXIifQ.c2lnbmF0dXJlLXZhbHVl";

        assertRejected(Map.of("note", jwt));
        assertRejected(Map.of("details", List.of(Map.of("inner", Map.of("label", jwt)))));
    }

    @Test
    void testPlainValuesAreNotMistakenForSecrets() {
        AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

        appAuditPublisher.publish(
            AppAuditEvent.APP_DEPLOYMENT_COMPLETED, 1L,
            Map.of("versionName", "1.2.3", "provider", "VERCEL", "name", "Customer data portal", "environment",
                "PRODUCTION"));

        assertThatCode(() -> AppAuditDataAssertions.assertNoSecrets(appAuditPublisher)).doesNotThrowAnyException();
    }

    @Test
    void testTheRawKeyItsHashAndItsHintAreRejectedWhenPassedAsForbiddenValues() {
        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        for (String forbidden : List.of(generatedKey.key(), generatedKey.hash(), generatedKey.hint())) {
            AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

            appAuditPublisher.publish(
                AppAuditEvent.APP_SECRET_KEY_ROTATED, 1L,
                Map.of("trail", List.of(Map.of("deep", "ends " + forbidden))));

            assertThatThrownBy(() -> AppAuditDataAssertions.assertNoSecrets(
                appAuditPublisher, generatedKey.key(), generatedKey.hash(), generatedKey.hint()))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Test
    void testAShortForbiddenValueMatchesOnlyWholeValues() {
        AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

        appAuditPublisher.publish(AppAuditEvent.APP_INSTANCE_UPDATED, 1L, Map.of("environment", "DEVELOPMENT"));

        assertThatCode(() -> AppAuditDataAssertions.assertNoSecrets(appAuditPublisher, "DE"))
            .doesNotThrowAnyException();

        AppAuditPublisher leakingPublisher = mock(AppAuditPublisher.class);

        leakingPublisher.publish(AppAuditEvent.APP_INSTANCE_UPDATED, 1L, Map.of("country", "DE"));

        assertThatThrownBy(() -> AppAuditDataAssertions.assertNoSecrets(leakingPublisher, "DE"))
            .isInstanceOf(AssertionError.class);
    }

    private static void assertRejected(Map<String, Object> data) {
        AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);

        appAuditPublisher.publish(AppAuditEvent.APP_INSTANCE_UPDATED, 1L, data);

        assertThatThrownBy(() -> AppAuditDataAssertions.assertNoSecrets(appAuditPublisher))
            .isInstanceOf(AssertionError.class);
    }
}
