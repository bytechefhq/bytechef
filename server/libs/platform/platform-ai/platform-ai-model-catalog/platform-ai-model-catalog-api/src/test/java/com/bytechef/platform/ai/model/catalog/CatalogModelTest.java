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

package com.bytechef.platform.ai.model.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import org.junit.jupiter.api.Test;

class CatalogModelTest {

    @Test
    void testStatusFromWireValueMapsKnownValues() {
        assertThat(Status.fromWireValue("deprecated")).isEqualTo(Status.DEPRECATED);
        assertThat(Status.fromWireValue("beta")).isEqualTo(Status.BETA);
    }

    @Test
    void testStatusFromWireValueDefaultsToActive() {
        assertThat(Status.fromWireValue(null)).isEqualTo(Status.ACTIVE);
        assertThat(Status.fromWireValue("")).isEqualTo(Status.ACTIVE);
        assertThat(Status.fromWireValue("retired-next-tuesday")).isEqualTo(Status.ACTIVE);
    }

    @Test
    void testModalityFromWireValueMapsKnownValues() {
        assertThat(Modality.fromWireValue("text")).isEqualTo(Modality.TEXT);
        assertThat(Modality.fromWireValue("PDF")).isEqualTo(Modality.PDF);
    }

    @Test
    void testModalityFromWireValueReturnsNullForUnknownValue() {
        assertThat(Modality.fromWireValue("hologram")).isNull();
        assertThat(Modality.fromWireValue(null)).isNull();
    }
}
