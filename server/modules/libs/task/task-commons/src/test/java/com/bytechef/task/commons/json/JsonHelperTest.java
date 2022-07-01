/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.commons.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.TypeRef;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JsonHelperTest {

    private static final JsonHelper jsonHelper = new JsonHelper(new ObjectMapper());

    @Test
    public void testRead() {
        Assertions.assertThat((Boolean) jsonHelper.read("true")).isEqualTo(true);

        Assertions.assertThat(jsonHelper.read("true", Boolean.class)).isEqualTo(true);

        Assertions.assertThat((String) jsonHelper.read("\"c\"")).isEqualTo("c");

        Assertions.assertThat(jsonHelper.read("\"c\"", String.class)).isEqualTo("c");

        Assertions.assertThat((Integer) jsonHelper.read("2")).isEqualTo(2);

        Assertions.assertThat(jsonHelper.read("2", Integer.class)).isEqualTo(2);

        Assertions.assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> jsonHelper.read("item"));

        Assertions.assertThat((String) jsonHelper.read("\"item\"")).isEqualTo("item");

        Assertions.assertThat(jsonHelper.read("\"item\"", String.class)).isEqualTo("item");

        Assertions.assertThat(jsonHelper.read("[2,4]", List.class)).isEqualTo(List.of(2, 4));

        Assertions.assertThat(jsonHelper.read("[\"item1\",\"item2\"]", List.class))
                .isEqualTo(List.of("item1", "item2"));

        Assertions.assertThat((Map<?, ?>) jsonHelper.read("{\"key\":\"value\"}"))
                .isEqualTo(Map.of("key", "value"));

        Assertions.assertThat(jsonHelper.read("{\"key\":\"value\"}", Map.class)).isEqualTo(Map.of("key", "value"));

        Assertions.assertThat((List<?>) jsonHelper.read("[{\"key\":\"value\"}]"))
                .isEqualTo(List.of(Map.of("key", "value")));

        Assertions.assertThat((Map<?, ?>) jsonHelper.read(jsonHelper.write(Map.of(
                        "name",
                        "Poppy",
                        "color",
                        "RED",
                        "petals",
                        "9",
                        "id",
                        "45",
                        "Florists",
                        Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))))))
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

        Assertions.assertThat((Map<?, ?>) jsonHelper.read(
                        jsonHelper.write(Map.of(
                                "name",
                                "Poppy",
                                "color",
                                "RED",
                                "petals",
                                "9",
                                "id",
                                "45",
                                "Florists",
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark"))))),
                        Map.class))
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
        Assertions.assertThat((List<?>) jsonHelper.read("[2,4]")).isEqualTo(List.of(2, 4));

        Assertions.assertThat((List<?>) jsonHelper.read("[\"item1\",\"item2\"]"))
                .isEqualTo(List.of("item1", "item2"));

        Assertions.assertThat(jsonHelper.read("[{\"key\":\"value\"}]", List.class))
                .isEqualTo(List.of(Map.of("key", "value")));

        Assertions.assertThat((List<?>) jsonHelper.read(jsonHelper.write(List.of(
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
                        Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")))))
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

        Assertions.assertThat(jsonHelper.read(
                        jsonHelper.write(List.of(
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
                                Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46"))),
                        List.class))
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
        Assertions.assertThat(jsonHelper.read(
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
                        "$.cities",
                        new TypeRef<List<Map<String, ?>>>() {}))
                .isEqualTo(
                        jsonHelper.read(
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
                """));
    }

    @Test
    public void testWrite() {
        Assertions.assertThat(jsonHelper.write(true)).isEqualTo("true");

        Assertions.assertThat(jsonHelper.write('c')).isEqualTo("\"c\"");

        Assertions.assertThat(jsonHelper.write(2)).isEqualTo("2");

        Assertions.assertThat(jsonHelper.write("item")).isEqualTo("\"item\"");

        Assertions.assertThat(jsonHelper.write(Map.of("key", "value"))).isEqualTo("{\"key\":\"value\"}");

        Assertions.assertThat(jsonHelper.read(
                        jsonHelper.write(Map.of(
                                "name",
                                "Poppy",
                                "color",
                                "RED",
                                "petals",
                                "9",
                                "id",
                                "45",
                                "Florists",
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark"))))),
                        Map.class))
                .isEqualTo(
                        jsonHelper.read(
                                """
                    {"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"}
                    """));

        Assertions.assertThat(jsonHelper.read(
                        jsonHelper.write(Map.of(
                                "name",
                                "Poppy",
                                "color",
                                "RED",
                                "petals",
                                "9",
                                "id",
                                "45",
                                "Florists",
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark"))))),
                        Map.class))
                .isEqualTo(
                        jsonHelper.read(
                                """
                 {"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"}
                 """));
    }

    @Test
    public void testWriteArray() {
        Assertions.assertThat(jsonHelper.write(List.of(2, 4))).isEqualTo("[2,4]");

        Assertions.assertThat(jsonHelper.write(List.of("item1", "item2"))).isEqualTo("[\"item1\",\"item2\"]");

        Assertions.assertThat(jsonHelper.write(List.of(Map.of("key", "value")))).isEqualTo("[{\"key\":\"value\"}]");

        Assertions.assertThat(jsonHelper.read(
                        jsonHelper.write(List.of(
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
                                Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46"))),
                        List.class))
                .isEqualTo(
                        jsonHelper.read(
                                """
                [{"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"},{"color":"YELLOW", "id":"46", "name":"Rose", "petals":"5"}]
                """));

        Assertions.assertThat(jsonHelper.read(
                        jsonHelper.write(List.of(
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
                                Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46"))),
                        List.class))
                .isEqualTo(
                        jsonHelper.read(
                                """
                  [{"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"},{"color":"YELLOW", "id":"46", "name":"Rose", "petals":"5"}]
                  """));
    }
}
