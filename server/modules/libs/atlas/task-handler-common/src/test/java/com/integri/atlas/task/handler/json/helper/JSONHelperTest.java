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

package com.integri.atlas.task.handler.json.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JSONHelperTest {

    private static final JSONHelper jsonHelper = new JSONHelper(new ObjectMapper());

    @Test
    public void testDeserialize() {
        Assertions.assertThat((Boolean) jsonHelper.deserialize("true")).isEqualTo(true);

        Assertions.assertThat(jsonHelper.deserialize("true", Boolean.class)).isEqualTo(true);

        Assertions.assertThat((String) jsonHelper.deserialize("\"c\"")).isEqualTo("c");

        Assertions.assertThat(jsonHelper.deserialize("\"c\"", String.class)).isEqualTo("c");

        Assertions.assertThat((Integer) jsonHelper.deserialize("2")).isEqualTo(2);

        Assertions.assertThat(jsonHelper.deserialize("2", Integer.class)).isEqualTo(2);

        Assertions.assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> jsonHelper.deserialize("item"));

        Assertions.assertThat((String) jsonHelper.deserialize("\"item\"")).isEqualTo("item");

        Assertions.assertThat(jsonHelper.deserialize("\"item\"", String.class)).isEqualTo("item");

        Assertions.assertThat((List<?>) jsonHelper.deserialize("[2,4]")).isEqualTo(List.of(2, 4));

        Assertions.assertThat(jsonHelper.deserialize("[2,4]", List.class)).isEqualTo(List.of(2, 4));

        Assertions
            .assertThat((List<?>) jsonHelper.deserialize("[\"item1\",\"item2\"]"))
            .isEqualTo(List.of("item1", "item2"));

        Assertions
            .assertThat(jsonHelper.deserialize("[\"item1\",\"item2\"]", List.class))
            .isEqualTo(List.of("item1", "item2"));

        Assertions
            .assertThat((Map<?, ?>) jsonHelper.deserialize("{\"key\":\"value\"}"))
            .isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat(jsonHelper.deserialize("{\"key\":\"value\"}", Map.class))
            .isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat((List<?>) jsonHelper.deserialize("[{\"key\":\"value\"}]"))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions
            .assertThat(jsonHelper.deserialize("[{\"key\":\"value\"}]", List.class))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions
            .assertThat(
                (Map<?, ?>) jsonHelper.deserialize(
                    jsonHelper.serialize(
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
                            Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                        )
                    )
                )
            )
            .isEqualTo(
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
                    Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                )
            );

        Assertions
            .assertThat(
                (Map<?, ?>) jsonHelper.deserialize(
                    jsonHelper.serialize(
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
                            Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                        )
                    ),
                    Map.class
                )
            )
            .isEqualTo(
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
                    Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                )
            );

        Assertions
            .assertThat(
                (List<?>) jsonHelper.deserialize(
                    jsonHelper.serialize(
                        List.of(
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
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                            ),
                            Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                        )
                    )
                )
            )
            .isEqualTo(
                List.of(
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
                        Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                    ),
                    Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                )
            );

        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
                        List.of(
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
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                            ),
                            Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                        )
                    ),
                    List.class
                )
            )
            .isEqualTo(
                List.of(
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
                        Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                    ),
                    Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                )
            );
    }

    @Test
    public void testCheckJSON() {
        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON(true));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON(true, Boolean.class));

        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> jsonHelper.checkJSON(2));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON(2, Integer.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON("2"));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON("2", String.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON("item"));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSON("item", String.class));

        Assertions.assertThat((List<?>) jsonHelper.checkJSON("[2,4]")).isEqualTo(List.of(2, 4));

        Assertions.assertThat(jsonHelper.checkJSON("[2,4]", List.class)).isEqualTo(List.of(2, 4));

        Assertions
            .assertThat((List<?>) jsonHelper.checkJSON("[\"item1\",\"item2\"]"))
            .isEqualTo(List.of("item1", "item2"));

        Assertions
            .assertThat(jsonHelper.checkJSON("[\"item1\",\"item2\"]", List.class))
            .isEqualTo(List.of("item1", "item2"));

        Assertions
            .assertThat((Map<?, ?>) jsonHelper.checkJSON(Map.of("key", "value")))
            .isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat(jsonHelper.checkJSON(Map.of("key", "value"), Map.class))
            .isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat((List<?>) jsonHelper.checkJSON(List.of(Map.of("key", "value"))))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions
            .assertThat(jsonHelper.checkJSON(List.of(Map.of("key", "value")), List.class))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions
            .assertThat((Map<?, ?>) jsonHelper.checkJSON(jsonHelper.serialize(Map.of("key", "value"))))
            .isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat(jsonHelper.checkJSON(jsonHelper.serialize(Map.of("key", "value")), Map.class))
            .isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat(
                (List<?>) jsonHelper.checkJSON(
                    jsonHelper.serialize(List.of(Map.of("key1", "value1"), Map.of("key2", "value2")))
                )
            )
            .isEqualTo(List.of(Map.of("key1", "value1"), Map.of("key2", "value2")));

        Assertions
            .assertThat(
                jsonHelper.checkJSON(
                    jsonHelper.serialize(List.of(Map.of("key1", "value1"), Map.of("key2", "value2"))),
                    List.class
                )
            )
            .isEqualTo(List.of(Map.of("key1", "value1"), Map.of("key2", "value2")));
    }

    @Test
    public void testCheckJSONArray() {
        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray(true, new TypeReference<>() {}));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray(true, Boolean.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray(2, new TypeReference<>() {}));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray(2, Integer.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray("2", new TypeReference<>() {}));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray("2", String.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray("item", new TypeReference<>() {}));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray("item", String.class));

        List<Integer> list = jsonHelper.checkJSONArray("[2,4]", new TypeReference<>() {});

        Assertions.assertThat(list).containsAll(List.of(2, 4));

        Assertions.assertThat(jsonHelper.checkJSONArray("[2,4]", Integer.class)).containsAll(List.of(2, 4));

        Assertions
            .assertThat(jsonHelper.checkJSONArray("[\"item1\",\"item2\"]", String.class))
            .isEqualTo(List.of("item1", "item2"));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray(Map.of("key", "value"), Map.class));

        Assertions
            .assertThat(jsonHelper.checkJSONArray(List.of(Map.of("key", "value")), Map.class))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions.assertThat(jsonHelper.checkJSONArray(Set.of(2, 3), Integer.class)).containsAll(List.of(2, 3));

        Assertions
            .assertThat(jsonHelper.checkJSONArray(Set.of(Map.of("key", "value")), Map.class))
            .isEqualTo(List.of(Map.of("key", "value")));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONArray(jsonHelper.serialize(Map.of("key", "value")), Map.class));

        Assertions
            .assertThat(
                jsonHelper.checkJSONArray(
                    jsonHelper.serialize(List.of(Map.of("key1", "value1"), Map.of("key2", "value2"))),
                    Map.class
                )
            )
            .containsAll(List.of(Map.of("key1", "value1"), Map.of("key2", "value2")));
    }

    @Test
    public void testCheckJSONObject() {
        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject(true, Boolean.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject(2, Integer.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject("2", String.class));

        Assertions
            .assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject("item", String.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject("[2,4]", String.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject("[\"item1\",\"item2\"]", String.class));

        Assertions
            .assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> jsonHelper.checkJSONObject(List.of(Map.of("key", "value")), String.class));

        Assertions
            .assertThat(jsonHelper.checkJSONObject(jsonHelper.serialize(Map.of("key", "value"))))
            .isEqualTo(Map.of("key", "value"));

        Map<String, String> map = jsonHelper.checkJSONObject(Map.of("key", "value"), new TypeReference<>() {});

        Assertions.assertThat(map).isEqualTo(Map.of("key", "value"));

        Assertions
            .assertThat(jsonHelper.checkJSONObject(jsonHelper.serialize(Map.of("key", "value")), String.class))
            .isEqualTo(Map.of("key", "value"));
    }

    @Test
    public void testSerialize() {
        Assertions.assertThat(jsonHelper.serialize(true)).isEqualTo("true");

        Assertions.assertThat(jsonHelper.serialize('c')).isEqualTo("\"c\"");

        Assertions.assertThat(jsonHelper.serialize(2)).isEqualTo("2");

        Assertions.assertThat(jsonHelper.serialize("item")).isEqualTo("\"item\"");

        Assertions.assertThat(jsonHelper.serialize(List.of(2, 4))).isEqualTo("[2,4]");

        Assertions.assertThat(jsonHelper.serialize(List.of("item1", "item2"))).isEqualTo("[\"item1\",\"item2\"]");

        Assertions.assertThat(jsonHelper.serialize(Map.of("key", "value"))).isEqualTo("{\"key\":\"value\"}");

        Assertions.assertThat(jsonHelper.serialize(List.of(Map.of("key", "value")))).isEqualTo("[{\"key\":\"value\"}]");

        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
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
                            Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                        )
                    ),
                    Map.class
                )
            )
            .isEqualTo(
                jsonHelper.deserialize(
                    """
                    {"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"}
                    """
                )
            );

        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
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
                            Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                        )
                    ),
                    Map.class
                )
            )
            .isEqualTo(
                jsonHelper.deserialize(
                    """
                 {"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"}
                 """
                )
            );

        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
                        List.of(
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
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                            ),
                            Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                        )
                    ),
                    List.class
                )
            )
            .isEqualTo(
                jsonHelper.deserialize(
                    """
                [{"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"},{"color":"YELLOW", "id":"46", "name":"Rose", "petals":"5"}]
                """
                )
            );

        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
                        List.of(
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
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                            ),
                            Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                        )
                    ),
                    List.class
                )
            )
            .isEqualTo(
                jsonHelper.deserialize(
                    """
                  [{"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"},{"color":"YELLOW", "id":"46", "name":"Rose", "petals":"5"}]
                  """
                )
            );
    }
}
