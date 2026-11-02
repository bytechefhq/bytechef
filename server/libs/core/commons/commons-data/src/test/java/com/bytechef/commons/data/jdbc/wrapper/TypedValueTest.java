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

package com.bytechef.commons.data.jdbc.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class TypedValueTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    @Test
    void testWriteAndReadKeepTheValueClass() {
        TypedValue typedValue = new TypedValue(LocalDate.of(2026, 11, 2));

        TypedValue readTypedValue = TypedValue.read(objectMapper, typedValue.write(objectMapper));

        assertThat(readTypedValue.classname()).isEqualTo("java.time.LocalDate");
        assertThat(readTypedValue.value()).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void testReadKeepsTheStoredJsonShape() {
        TypedValue typedValue = TypedValue.read(
            objectMapper, "{\"value\":{\"key\":\"value\"},\"classname\":\"java.util.HashMap\"}");

        assertThat(typedValue.value()).isEqualTo(Map.of("key", "value"));
    }

    @Test
    void testWriteAndReadKeepArrays() {
        TypedValue stringArray = TypedValue.read(
            objectMapper, new TypedValue(new String[] {
                "a", "b"
            }).write(objectMapper));
        TypedValue intArray = TypedValue.read(objectMapper, new TypedValue(new int[] {
            1, 2
        }).write(objectMapper));
        TypedValue nestedArray = TypedValue.read(
            objectMapper, new TypedValue(new Long[][] {
                {
                    1L
                }, {
                    2L, 3L
                }
            }).write(objectMapper));

        assertThat(stringArray.value()).isEqualTo(new String[] {
            "a", "b"
        });
        assertThat(intArray.value()).isEqualTo(new int[] {
            1, 2
        });
        assertThat(nestedArray.value()).isEqualTo(new Long[][] {
            {
                1L
            }, {
                2L, 3L
            }
        });
    }

    @Test
    void testReadRejectsAnArrayOfAClassOutsideTheAllowedPackages() {
        assertThatThrownBy(
            () -> TypedValue.read(objectMapper, "{\"value\":[],\"classname\":\"[Lorg.example.Payload;\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("org.example.Payload");
    }

    @Test
    void testReadRejectsAClassOutsideTheAllowedPackages() {
        assertThatThrownBy(
            () -> TypedValue.read(objectMapper, "{\"value\":\"x\",\"classname\":\"org.example.Payload\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("org.example.Payload");
    }

    @Test
    void testReadRejectsAnUnknownClass() {
        assertThatThrownBy(
            () -> TypedValue.read(objectMapper, "{\"value\":\"x\",\"classname\":\"com.bytechef.Missing\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("com.bytechef.Missing");
    }
}
