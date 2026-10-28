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

import com.stripe.model.Price;
import java.util.Map;

/**
 * @author Matija Petanjek
 */
public final class PrepaidPriceUtils {

    private static final String UNIT_TOTAL_KEY = "unit_total";

    private PrepaidPriceUtils() {
    }

    /**
     * Reads the number of units a price grants from its {@code unit_total} metadata.
     *
     * @throws IllegalStateException if the metadata is missing or not an integer
     */
    public static int getUnitTotal(Price price) {
        Map<String, String> metadata = price.getMetadata();

        String unitTotal = metadata == null ? null : metadata.get(UNIT_TOTAL_KEY);

        if (unitTotal == null) {
            throw new IllegalStateException("Price " + price.getId() + " is missing the 'unit_total' metadata");
        }

        try {
            return Integer.parseInt(unitTotal.trim());
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalStateException(
                "Price " + price.getId() + " has a non-integer 'unit_total' metadata: " + unitTotal,
                numberFormatException);
        }
    }
}
