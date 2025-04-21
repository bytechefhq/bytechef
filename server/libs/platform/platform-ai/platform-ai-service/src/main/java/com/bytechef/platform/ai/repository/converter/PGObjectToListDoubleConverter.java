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

package com.bytechef.platform.ai.repository.converter;

import java.util.Arrays;
import java.util.List;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * @author Marko Kriskovic
 */
@ReadingConverter
public class PGObjectToListDoubleConverter implements Converter<PGobject, List<Double>> {

    @Override
    public List<Double> convert(PGobject source) {
        try {
            String vectorString = source.getValue();

            if (vectorString == null) {
                throw new IllegalArgumentException("PGobject value cannot be null");
            }

            if (vectorString.startsWith("[") && vectorString.endsWith("]")) {
                vectorString = vectorString.substring(1, vectorString.length() - 1);
            }

            return Arrays.stream(vectorString.split(","))
                .map(Double::parseDouble) // Convert to Double
                .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PGobject to List<Double>", e);
        }
    }
}
