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

package com.integri.atlas.test.json;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.integri.atlas.test.json.JSONArrayUtil;
import com.integri.atlas.test.json.JSONObjectUtil;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JSONArrayUtilTest {

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
}
