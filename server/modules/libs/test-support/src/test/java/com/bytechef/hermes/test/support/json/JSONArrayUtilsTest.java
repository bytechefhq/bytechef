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

package com.bytechef.hermes.test.support.json;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.test.support.json.JSONArrayUtils;
import com.bytechef.test.support.json.JSONObjectUtils;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JSONArrayUtilsTest {

    @Test
    public void testJSONArrayB() {
        assertEquals(
                new JSONArray().put(new JSONObject().put("key", "value")),
                JSONArrayUtils.of("""
            [{"key": "value"}]
            """),
                true);

        assertEquals(
                new JSONArray().put(new JSONObject().put("key1", "value1")).put(new JSONObject().put("key2", "value2")),
                JSONArrayUtils.of(JSONObjectUtils.of("key1", "value1"), JSONObjectUtils.of("key2", "value2")),
                true);

        assertEquals(
                new JSONArray().put(new JSONObject().put("key", "value")),
                JSONArrayUtils.of(JSONArrayUtils.of(JSONObjectUtils.of("key", "value"))),
                true);

        assertEquals(
                new JSONArray().put(new JSONObject().put("key", "value")),
                JSONArrayUtils.of(List.of(JSONObjectUtils.of("key", "value"))),
                true);
    }
}
