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

package com.bytechef.platform.billing.handler.prepaid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stripe.model.Price;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Matija Petanjek
 */
class PrepaidPriceUtilsTest {

    @Test
    void testGetUnitTotalParsesInteger() {
        assertThat(PrepaidPriceUtils.getUnitTotal(price(Map.of("unit_total", "5000")))).isEqualTo(5000);
    }

    @Test
    void testGetUnitTotalThrowsWhenMetadataKeyMissing() {
        assertThatThrownBy(() -> PrepaidPriceUtils.getUnitTotal(price(Map.of())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("price_test");
    }

    @Test
    void testGetUnitTotalThrowsWhenNotAnInteger() {
        assertThatThrownBy(() -> PrepaidPriceUtils.getUnitTotal(price(Map.of("unit_total", "abc"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("price_test");
    }

    private Price price(Map<String, String> metadata) {
        Price price = new Price();

        price.setId("price_test");
        price.setMetadata(metadata);

        return price;
    }
}
