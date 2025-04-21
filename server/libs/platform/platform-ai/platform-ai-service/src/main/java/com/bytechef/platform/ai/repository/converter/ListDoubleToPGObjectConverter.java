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

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * @author Marko Kriskovic
 */
@WritingConverter
public class ListDoubleToPGObjectConverter implements Converter<List<Double>, PGobject> {

    @Override
    public PGobject convert(List<Double> source) {
        try {
            PGobject pgObject = new PGobject();

            pgObject.setType("vector(1536)");

            // Convert List<Double> to a comma-separated string representation
            String vectorString = source.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

            pgObject.setValue(vectorString);

            return pgObject;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to convert List<Double> to PGobject", e);
        }
    }
}
