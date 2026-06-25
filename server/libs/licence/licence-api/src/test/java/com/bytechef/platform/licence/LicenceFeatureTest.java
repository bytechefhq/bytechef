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

package com.bytechef.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LicenceFeatureTest {

    @Test
    void testOfKeyResolvesKnownKey() {
        assertThat(LicenceFeature.ofKey("sso")).contains(LicenceFeature.SSO);
    }

    @Test
    void testOfKeyReturnsEmptyForUnknownKey() {
        assertThat(LicenceFeature.ofKey("does-not-exist")).isEmpty();
    }

    @Test
    void testEveryFeatureHasUniqueLowerCaseKey() {
        long distinct = java.util.Arrays.stream(LicenceFeature.values())
            .map(LicenceFeature::getKey)
            .distinct()
            .count();

        assertThat(distinct).isEqualTo(LicenceFeature.values().length);
    }

    @Test
    void testStatusIsActive() {
        assertThat(LicenceStatus.VALID.isActive()).isTrue();
        assertThat(LicenceStatus.GRACE.isActive()).isTrue();
        assertThat(LicenceStatus.EXPIRED.isActive()).isFalse();
        assertThat(LicenceStatus.MISSING.isActive()).isFalse();
        assertThat(LicenceStatus.INVALID.isActive()).isFalse();
        assertThat(LicenceStatus.CE.isActive()).isFalse();
    }
}
