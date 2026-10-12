/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.bytechef.commons.util.EncodingUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantKeyedTokenValuesTest {

    @Test
    void testGeneratedValueCarriesTenantAndIsUrlSafe() {
        String value = TenantKeyedTokenValues.generate("acme");

        assertThat(value).matches("[A-Za-z0-9_-]+");
        assertThat(TenantKeyedTokenValues.parseTenantId(value)).contains("acme");
        assertThat(TenantKeyedTokenValues.generate("acme")).isNotEqualTo(value);
    }

    @Test
    void testGenerateRejectsTenantIdsThatCouldNotBeParsedBack() {
        assertThatIllegalArgumentException().isThrownBy(() -> TenantKeyedTokenValues.generate("ac:me"));
        assertThatIllegalArgumentException().isThrownBy(() -> TenantKeyedTokenValues.generate("public; DROP TABLE x"));
        assertThatIllegalArgumentException().isThrownBy(() -> TenantKeyedTokenValues.generate(""));
    }

    @Test
    void testParseRejectsForeignValues() {
        assertThat(TenantKeyedTokenValues.parseTenantId("eyJhbGciOiJSUzI1NiJ9.e30.sig")).isEmpty();
        assertThat(TenantKeyedTokenValues.parseTenantId("not base64 !")).isEmpty();
        assertThat(TenantKeyedTokenValues.parseTenantId("")).isEmpty();
        assertThat(TenantKeyedTokenValues.parseTenantId(null)).isEmpty();
    }

    @Test
    void testParseRejectsTenantIdsThatAreNotSchemaSafe() {
        String injected = EncodingUtils.urlEncodeBase64ToString("public; DROP TABLE x:random");
        String quoted = EncodingUtils.urlEncodeBase64ToString("ac\"me:random");

        assertThat(TenantKeyedTokenValues.parseTenantId(injected)).isEmpty();
        assertThat(TenantKeyedTokenValues.parseTenantId(quoted)).isEmpty();
    }

    @Test
    void testIsValidTenantIdAcceptsOnlySchemaSafeIds() {
        assertThat(TenantKeyedTokenValues.isValidTenantId("acme_1-x")).isTrue();
        assertThat(TenantKeyedTokenValues.isValidTenantId("public; DROP TABLE x")).isFalse();
        assertThat(TenantKeyedTokenValues.isValidTenantId("")).isFalse();
        assertThat(TenantKeyedTokenValues.isValidTenantId(null)).isFalse();
    }
}
