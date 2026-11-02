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

import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record Modalities(List<Modality> input, List<Modality> output) {

    public Modalities {
        input = List.copyOf(input);
        output = List.copyOf(output);
    }

    public enum Modality {

        TEXT, IMAGE, AUDIO, VIDEO, PDF;

        public static @Nullable Modality fromWireValue(@Nullable String wireValue) {
            if (wireValue == null) {
                return null;
            }

            for (Modality modality : values()) {
                if (modality.name()
                    .equals(wireValue.toUpperCase(Locale.ROOT))) {

                    return modality;
                }
            }

            return null;
        }
    }
}
