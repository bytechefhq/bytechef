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
import com.atlas.json.converter.JSONArrayConverter;
import com.atlas.json.serializer.JSONArrayStdSerializer;
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
public class JSONArrayUtilTest {

    private static final ConversionService conversionService = new DefaultConversionService() {
        {
            addConverter(new JSONArrayConverter());
        }
    };

    private static final ObjectMapper objectMapper = new ObjectMapper() {
        {
            SimpleModule simpleModule = new SimpleModule();

            simpleModule.addSerializer(JSONArray.class, new JSONArrayStdSerializer());

            registerModule(simpleModule);
        }
    };

    @Test
    public void testJSONArrayB() {
        assertEquals(
            new JSONArray().put(new JSONObject().put("key", "value")),
            JSONArrayUtil.of("""
            [{"key": "value"}]
            """),
            true
        );

        assertEquals(
            new JSONArray().put(new JSONObject().put("key1", "value1")).put(new JSONObject().put("key2", "value2")),
            JSONArrayUtil.of(JSONObjectUtil.of("key1", "value1"), JSONObjectUtil.of("key2", "value2")),
            true
        );

        assertEquals(
            new JSONArray().put(new JSONObject().put("key", "value")),
            JSONArrayUtil.of(JSONArrayUtil.of(JSONObjectUtil.of("key", "value"))),
            true
        );

        assertEquals(
            new JSONArray().put(new JSONObject().put("key", "value")),
            JSONArrayUtil.of(List.of(JSONObjectUtil.of("key", "value"))),
            true
        );
    }

    @Test
    public void testJacksonSerialization() throws JsonProcessingException {
        assertEquals(
            """
            [{"key1": "value1"}, {"key2": "value2"}]""",
            objectMapper.writeValueAsString(
                JSONArrayUtil.of(JSONObjectUtil.of("key1", "value1"), JSONObjectUtil.of("key2", "value2"))
            ),
            true
        );
    }

    @Test
    public void testConversion() {
        assertEquals(
            JSONArrayUtil.of(JSONObjectUtil.of("key1", "value1"), JSONObjectUtil.of("key2", "value2")),
            conversionService.convert(List.of(Map.of("key1", "value1"), Map.of("key2", "value2")), JSONArray.class),
            true
        );
    }
}
