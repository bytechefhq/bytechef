/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppSecretKeyRedactionTest {

    private static final String SECRET_KEY = "plaintext-secret-key-value";

    @Test
    void appSecretKeyDTOToStringRedactsTheKey() {
        assertRedacted(new AppSecretKeyDTO(null, SECRET_KEY).toString());
    }

    @Test
    void createdAppToStringRedactsTheKey() {
        assertRedacted(new AppService.CreatedApp(new App(), SECRET_KEY).toString());
    }

    @Test
    void rotatedKeyToStringRedactsTheKey() {
        assertRedacted(new AppService.RotatedKey(new App(), SECRET_KEY).toString());
    }

    @Test
    void generatedKeyToStringRedactsTheKeyButKeepsHashAndHint() {
        String value = new AppSecretKeys.GeneratedKey(SECRET_KEY, "hash-value", "hint1234").toString();

        assertRedacted(value);

        assertThat(value).contains("hash-value", "hint1234");
    }

    private static void assertRedacted(String value) {
        assertThat(value).doesNotContain(SECRET_KEY)
            .contains("[REDACTED]");
    }
}
