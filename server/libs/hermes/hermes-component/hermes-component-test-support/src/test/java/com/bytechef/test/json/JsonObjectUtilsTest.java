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

package com.bytechef.test.json;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.hermes.component.test.json.JsonArrayUtils;
import com.bytechef.hermes.component.test.json.JsonObjectUtils;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JsonObjectUtilsTest {

    @Test
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testOf() {
        assertEquals(new JSONObject().put("key", true), JsonObjectUtils.of("key", true), true);

        assertEquals(new JSONObject().put("key", 1), JsonObjectUtils.of("key", 1), true);

        assertEquals(new JSONObject().put("key", 2.1), JsonObjectUtils.of("key", 2.1), true);

        assertEquals(new JSONObject().put("key", 3.2F), JsonObjectUtils.of("key", 3.2F), true);

        assertEquals(new JSONObject().put("key", 2L), JsonObjectUtils.of("key", 2L), true);

        assertEquals(new JSONObject().put("key", "value"), JsonObjectUtils.of("key", "value"), true);

        assertEquals(
                new JSONObject().put("key1", "value1").put("key2", "value2"),
                JsonObjectUtils.of("key1", "value1", "key2", "value2"),
                true);

        assertEquals(
                new JSONObject().put("key1", "value1").put("key2", "value2").put("key3", 1),
                JsonObjectUtils.of("key1", "value1", "key2", "value2", "key3", 1),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1),
                JsonObjectUtils.of("key1", "value1", "key2", "value2", "key3", 1, "key4", 1.1),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1)
                        .put("key5", true),
                JsonObjectUtils.of("key1", "value1", "key2", "value2", "key3", 1, "key4", 1.1, "key5", true),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1)
                        .put("key5", true)
                        .put("key6", "value6"),
                JsonObjectUtils.of(
                        "key1", "value1", "key2", "value2", "key3", 1, "key4", 1.1, "key5", true, "key6", "value6"),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1)
                        .put("key5", true)
                        .put("key6", "value6")
                        .put("key7", new JSONObject().put("key1", "value1")),
                JsonObjectUtils.of(
                        "key1",
                        "value1",
                        "key2",
                        "value2",
                        "key3",
                        1,
                        "key4",
                        1.1,
                        "key5",
                        true,
                        "key6",
                        "value6",
                        "key7",
                        JsonObjectUtils.of("key1", "value1")),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1)
                        .put("key5", true)
                        .put("key6", "value6")
                        .put("key7", new JSONObject().put("key1", "value1"))
                        .put("key8", new JSONArray(List.of("value1", "value2"))),
                JsonObjectUtils.of(
                        "key1",
                        "value1",
                        "key2",
                        "value2",
                        "key3",
                        1,
                        "key4",
                        1.1,
                        "key5",
                        true,
                        "key6",
                        "value6",
                        "key7",
                        JsonObjectUtils.of("key1", "value1"),
                        "key8",
                        JsonArrayUtils.of("value1", "value2")),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1)
                        .put("key5", true)
                        .put("key6", "value6")
                        .put("key7", new JSONObject().put("key1", "value1"))
                        .put("key8", new JSONArray(List.of("value1", "value2")))
                        .put("key9", 5.3F),
                JsonObjectUtils.of(
                        "key1",
                        "value1",
                        "key2",
                        "value2",
                        "key3",
                        1,
                        "key4",
                        1.1,
                        "key5",
                        true,
                        "key6",
                        "value6",
                        "key7",
                        JsonObjectUtils.of("key1", "value1"),
                        "key8",
                        JsonArrayUtils.of("value1", "value2"),
                        "key9",
                        5.3F),
                true);

        assertEquals(
                new JSONObject()
                        .put("key1", "value1")
                        .put("key2", "value2")
                        .put("key3", 1)
                        .put("key4", 1.1)
                        .put("key5", true)
                        .put("key6", "value6")
                        .put("key7", new JSONObject().put("key1", "value1"))
                        .put("key8", new JSONArray(List.of("value1", "value2")))
                        .put("key9", 5.3F)
                        .put("key10", 5L),
                JsonObjectUtils.of(
                        "key1",
                        "value1",
                        "key2",
                        "value2",
                        "key3",
                        1,
                        "key4",
                        1.1,
                        "key5",
                        true,
                        "key6",
                        "value6",
                        "key7",
                        JsonObjectUtils.of("key1", "value1"),
                        "key8",
                        JsonArrayUtils.of("value1", "value2"),
                        "key9",
                        5.3F,
                        "key10",
                        5L),
                true);

        assertEquals(
                new JSONObject().put("key", Map.of("key", "value")),
                JsonObjectUtils.of("key", Map.of("key", "value")),
                true);

        assertEquals(new JSONObject().put("key", List.of("value")), JsonObjectUtils.of("key", List.of("value")), true);

        assertEquals(
                new JSONObject().put("key", JsonObjectUtils.of("key", "value")),
                JsonObjectUtils.of("key", JsonObjectUtils.of("key", "value")),
                true);

        assertEquals(new JSONObject().put("column_1", "value"), JsonObjectUtils.of(List.of("value")), true);

        assertEquals(
                new JSONObject().put("column_1", "value1"),
                JsonObjectUtils.of(List.of("value"), value -> value + 1),
                true);

        assertEquals(new JSONObject().put("key", "value"), JsonObjectUtils.of(Map.of("key", "value")), true);

        assertEquals(
                new JSONObject().put("key", "value1"),
                JsonObjectUtils.of(Map.of("key", "value"), value -> value + 1),
                true);
    }
}
