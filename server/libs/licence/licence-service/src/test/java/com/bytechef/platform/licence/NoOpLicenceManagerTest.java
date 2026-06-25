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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NoOpLicenceManagerTest {

    private final NoOpLicenceManager licenceManager = new NoOpLicenceManager();

    @Test
    void testStatusIsCe() {
        assertThat(licenceManager.getStatus()).isEqualTo(LicenceStatus.CE);
    }

    @Test
    void testAllowedJobsUnlimited() {
        assertThat(licenceManager.getAllowedJobs()).isEqualTo(-1L);
    }

    @Test
    void testNoFeaturesEnabled() {
        assertThat(licenceManager.isFeatureEnabled(LicenceFeature.SSO)).isFalse();
    }

    @Test
    void testCheckFeatureThrows() {
        assertThatThrownBy(() -> licenceManager.checkFeature(LicenceFeature.SSO))
            .isInstanceOf(LicenceException.class);
    }

    @Test
    void testGetLicenceEmpty() {
        assertThat(licenceManager.getLicence()).isEmpty();
    }

    @Test
    void testUploadUnsupported() {
        assertThatThrownBy(() -> licenceManager.upload(new byte[0]))
            .isInstanceOf(LicenceException.class);
    }

    @Test
    void testDeleteUnsupported() {
        assertThatThrownBy(() -> licenceManager.delete())
            .isInstanceOf(LicenceException.class);
    }
}
