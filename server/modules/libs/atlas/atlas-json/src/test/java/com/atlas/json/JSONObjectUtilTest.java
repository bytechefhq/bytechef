/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlas.json;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.atlas.json.JSONArrayUtil;
import com.atlas.json.JSONObjectUtil;
import com.atlas.json.converter.JSONObjectConverter;
import com.atlas.json.serializer.JSONObjectStdSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * @author Ivica Cardic
 */
public class JSONObjectUtilTest {

    private static final ConversionService conversionService = new DefaultConversionService() {
        {
            addConverter(new JSONObjectConverter());
        }
    };

    private static final ObjectMapper objectMapper = new ObjectMapper() {
        {
            SimpleModule simpleModule = new SimpleModule();

            simpleModule.addSerializer(JSONObject.class, new JSONObjectStdSerializer());

            registerModule(simpleModule);
        }
    };

    @Test
    public void testOf() {
        assertEquals(new JSONObject().put("key", true), JSONObjectUtil.of("key", true), true);

        assertEquals(new JSONObject().put("key", 1), JSONObjectUtil.of("key", 1), true);

        assertEquals(new JSONObject().put("key", 2.1), JSONObjectUtil.of("key", 2.1), true);

        assertEquals(new JSONObject().put("key", 3.2F), JSONObjectUtil.of("key", 3.2F), true);

        assertEquals(new JSONObject().put("key", 2L), JSONObjectUtil.of("key", 2L), true);

        assertEquals(new JSONObject().put("key", "value"), JSONObjectUtil.of("key", "value"), true);

        assertEquals(
            new JSONObject().put("key1", "value1").put("key2", "value2"),
            JSONObjectUtil.of("key1", "value1", "key2", "value2"),
            true
        );

        assertEquals(
            new JSONObject().put("key1", "value1").put("key2", "value2").put("key3", 1),
            JSONObjectUtil.of("key1", "value1", "key2", "value2", "key3", 1),
            true
        );

        assertEquals(
            new JSONObject().put("key1", "value1").put("key2", "value2").put("key3", 1).put("key4", 1.1),
            JSONObjectUtil.of("key1", "value1", "key2", "value2", "key3", 1, "key4", 1.1),
            true
        );

        assertEquals(
            new JSONObject()
                .put("key1", "value1")
                .put("key2", "value2")
                .put("key3", 1)
                .put("key4", 1.1)
                .put("key5", true),
            JSONObjectUtil.of("key1", "value1", "key2", "value2", "key3", 1, "key4", 1.1, "key5", true),
            true
        );

        assertEquals(
            new JSONObject()
                .put("key1", "value1")
                .put("key2", "value2")
                .put("key3", 1)
                .put("key4", 1.1)
                .put("key5", true)
                .put("key6", "value6"),
            JSONObjectUtil.of(
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
                "value6"
            ),
            true
        );

        assertEquals(
            new JSONObject()
                .put("key1", "value1")
                .put("key2", "value2")
                .put("key3", 1)
                .put("key4", 1.1)
                .put("key5", true)
                .put("key6", "value6")
                .put("key7", new JSONObject().put("key1", "value1")),
            JSONObjectUtil.of(
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
                JSONObjectUtil.of("key1", "value1")
            ),
            true
        );

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
            JSONObjectUtil.of(
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
                JSONObjectUtil.of("key1", "value1"),
                "key8",
                JSONArrayUtil.of("value1", "value2")
            ),
            true
        );

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
            JSONObjectUtil.of(
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
                JSONObjectUtil.of("key1", "value1"),
                "key8",
                JSONArrayUtil.of("value1", "value2"),
                "key9",
                5.3F
            ),
            true
        );

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
            JSONObjectUtil.of(
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
                JSONObjectUtil.of("key1", "value1"),
                "key8",
                JSONArrayUtil.of("value1", "value2"),
                "key9",
                5.3F,
                "key10",
                5L
            ),
            true
        );

        assertEquals(
            new JSONObject().put("key", Map.of("key", "value")),
            JSONObjectUtil.of("key", Map.of("key", "value")),
            true
        );

        assertEquals(new JSONObject().put("key", List.of("value")), JSONObjectUtil.of("key", List.of("value")), true);

        assertEquals(
            new JSONObject().put("key", JSONObjectUtil.of("key", "value")),
            JSONObjectUtil.of("key", JSONObjectUtil.of("key", "value")),
            true
        );

        assertEquals(new JSONObject().put("column_1", "value"), JSONObjectUtil.of(List.of("value")), true);

        assertEquals(
            new JSONObject().put("column_1", "value1"),
            JSONObjectUtil.of(List.of("value"), value -> value + 1),
            true
        );

        assertEquals(new JSONObject().put("key", "value"), JSONObjectUtil.of(Map.of("key", "value")), true);

        assertEquals(
            new JSONObject().put("key", "value1"),
            JSONObjectUtil.of(Map.of("key", "value"), value -> value + 1),
            true
        );
    }

    @Test
    public void testJacksonSerialization() throws JsonProcessingException {
        assertEquals(
            """
           {"key": "value"}""",
            objectMapper.writeValueAsString(JSONObjectUtil.of("key", "value")),
            true
        );
    }

    @Test
    public void testConversion() {
        assertEquals(
            JSONObjectUtil.of("key", "value"),
            conversionService.convert(Map.of("key", "value"), JSONObject.class),
            true
        );
    }
}
