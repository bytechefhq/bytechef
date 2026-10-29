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

import java.time.LocalDate;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record CatalogModel(
    String id, String name, @Nullable String description, @Nullable String family, boolean attachment,
    boolean reasoning, boolean toolCall, boolean structuredOutput, boolean temperature, boolean openWeights,
    @Nullable String knowledge, @Nullable LocalDate releaseDate, @Nullable LocalDate lastUpdated, Status status,
    Modalities modalities, Limit limit, @Nullable Cost cost) {

    public enum Status {

        ACTIVE, BETA, DEPRECATED;

        public static Status fromWireValue(@Nullable String wireValue) {
            if (wireValue == null || wireValue.isBlank()) {
                return ACTIVE;
            }

            for (Status status : values()) {
                if (status.name()
                    .equals(wireValue.toUpperCase(Locale.ROOT))) {

                    return status;
                }
            }

            return ACTIVE;
        }
    }
}
