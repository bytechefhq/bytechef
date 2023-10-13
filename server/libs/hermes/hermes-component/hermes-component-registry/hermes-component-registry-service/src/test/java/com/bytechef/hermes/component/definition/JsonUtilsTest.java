/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.component.definition;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JsonUtilsTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testRead() {
        record Row(String key) {
        }

        List<Row> rows = JsonUtils.readList("[{\"key\":\"value\"}]", Row.class, OBJECT_MAPPER);

        Assertions.assertThat(rows)
            .isEqualTo(List.of(new Row("value")));

        Assertions.assertThat((Boolean) JsonUtils.read("true", OBJECT_MAPPER))
            .isEqualTo(true);

        Assertions.assertThat((String) JsonUtils.read("\"c\"", OBJECT_MAPPER))
            .isEqualTo("c");

        Assertions.assertThat((Integer) JsonUtils.read("2", OBJECT_MAPPER))
            .isEqualTo(2);

        Assertions.assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> JsonUtils.read("item", OBJECT_MAPPER));

        Assertions.assertThat((String) JsonUtils.read("\"item\"", OBJECT_MAPPER))
            .isEqualTo("item");

        Assertions.assertThat((Map<?, ?>) JsonUtils.read("{\"key\":\"value\"}", OBJECT_MAPPER))
            .isEqualTo(Map.of("key", "value"));

        Assertions.assertThat((List<?>) JsonUtils.read("[{\"key\":\"value\"}]", OBJECT_MAPPER))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions.assertThat((Map<?, ?>) JsonUtils.read(JsonUtils.write(Map.of(
            "name", "Poppy",
            "color", "RED",
            "petals", "9",
            "id", "45",
            "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))), OBJECT_MAPPER),
            OBJECT_MAPPER))
            .isEqualTo(Map.of(
                "name",
                "Poppy",
                "color",
                "RED",
                "petals",
                "9",
                "id",
                "45",
                "Florists",
                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))));
    }

    @Test
    public void testReadList() {
        Assertions.assertThat((List<?>) JsonUtils.read("[2,4]", OBJECT_MAPPER))
            .isEqualTo(List.of(2, 4));

        Assertions.assertThat((List<?>) JsonUtils.read("[\"item1\",\"item2\"]", OBJECT_MAPPER))
            .isEqualTo(List.of("item1", "item2"));

        Assertions.assertThat((List<?>) JsonUtils.read(JsonUtils.write(List.of(
            Map.of(
                "name", "Poppy",
                "color", "RED",
                "petals", "9",
                "id", "45",
                "Florists",
                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))),
            Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")), OBJECT_MAPPER), OBJECT_MAPPER))
            .isEqualTo(List.of(
                Map.of(
                    "name",
                    "Poppy",
                    "color",
                    "RED",
                    "petals",
                    "9",
                    "id",
                    "45",
                    "Florists",
                    Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))),
                Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")));
    }

    @Test
    public void testReadListFromPath() {
        List<Map<String, ?>> list = (List) JsonUtils.readList(
            """
                {
                    "id": 77,
                    "city": "B",
                    "name": "A",
                    "active": true,
                    "description": "C",
                    "date": "2021-12-07",
                    "sum": 11.2,
                    "cities": [
                        {
                            "id": 77,
                            "city": "B",
                            "name": "A",
                            "active": true,
                            "description": "C",
                            "date": "2021-12-07",
                            "sum": 11.2
                        },
                        {
                            "id": 4,
                            "city": "city1",
                            "name": "name1",
                            "active": false,
                            "description": "description1",
                            "date": "",
                            "sum": 12
                        },
                        {
                            "id": 2,
                            "city": "city2",
                            "name": "A",
                            "active": true,
                            "description": "",
                            "date": "2021-12-09",
                            "sum": ""
                        },
                        {
                            "id": 5678,
                            "city": "city3",
                            "name": "ABCD",
                            "active": false,
                            "description": "EFGH",
                            "date": "2021-12-10",
                            "sum": 13.23
                        }
                    ]
                }
                """,
            "$.cities", Map.class, OBJECT_MAPPER);

        Assertions.assertThat(list)
            .isEqualTo(
                JsonUtils.read(
                    """
                        [
                            {
                                "id": 77,
                                "city": "B",
                                "name": "A",
                                "active": true,
                                "description": "C",
                                "date": "2021-12-07",
                                "sum": 11.2
                            },
                            {
                                "id": 4,
                                "city": "city1",
                                "name": "name1",
                                "active": false,
                                "description": "description1",
                                "date": "",
                                "sum": 12
                            },
                            {
                                "id": 2,
                                "city": "city2",
                                "name": "A",
                                "active": true,
                                "description": "",
                                "date": "2021-12-09",
                                "sum": ""
                            },
                            {
                                "id": 5678,
                                "city": "city3",
                                "name": "ABCD",
                                "active": false,
                                "description": "EFGH",
                                "date": "2021-12-10",
                                "sum": 13.23
                            }
                        ]
                        """, OBJECT_MAPPER));
    }

    @Test
    public void testWrite() {
        Assertions.assertThat(JsonUtils.write(true, OBJECT_MAPPER))
            .isEqualTo("true");

        Assertions.assertThat(JsonUtils.write('c', OBJECT_MAPPER))
            .isEqualTo("\"c\"");

        Assertions.assertThat(JsonUtils.write(2, OBJECT_MAPPER))
            .isEqualTo("2");

        Assertions.assertThat(JsonUtils.write("item", OBJECT_MAPPER))
            .isEqualTo("\"item\"");

        Assertions.assertThat(JsonUtils.write(Map.of("key", "value"), OBJECT_MAPPER))
            .isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    public void testWriteArray() {
        Assertions.assertThat(JsonUtils.write(List.of(2, 4), OBJECT_MAPPER))
            .isEqualTo("[2,4]");

        Assertions.assertThat(JsonUtils.write(List.of("item1", "item2"), OBJECT_MAPPER))
            .isEqualTo("[\"item1\",\"item2\"]");

        Assertions.assertThat(JsonUtils.write(List.of(Map.of("key", "value")), OBJECT_MAPPER))
            .isEqualTo("[{\"key\":\"value\"}]");
    }
}
