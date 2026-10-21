/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppSecretKeysTest {

    @Test
    void generatedKeyCarriesTheTenantAndHashesDeterministically() {
        AppSecretKeys.GeneratedKey generated = TenantContext.callWithTenantId(
            "tenant42", () -> AppSecretKeys.generate());

        assertThat(TenantKey.parse(generated.key())
            .getTenantId()).isEqualTo("tenant42");
        assertThat(generated.hash()).hasSize(64)
            .matches("[0-9a-f]+")
            .isEqualTo(AppSecretKeys.hash(generated.key()));
    }

    @Test
    void hintIsTheLastEightCharactersWithoutPadding() {
        assertThat(AppSecretKeys.hint("abcdefghijklmnop==")).isEqualTo("ijklmnop");
        assertThat(AppSecretKeys.hint("short")).isEqualTo("short");
    }

    @Test
    void differentKeysHashDifferently() {
        assertThat(AppSecretKeys.hash("k1")).isNotEqualTo(AppSecretKeys.hash("k2"));
    }
}
