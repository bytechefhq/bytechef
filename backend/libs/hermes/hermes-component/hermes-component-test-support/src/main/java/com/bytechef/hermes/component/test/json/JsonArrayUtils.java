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

package com.bytechef.hermes.component.test.json;

import java.util.Collection;
import java.util.List;
import org.json.JSONArray;

/**
 * @author Ivica Cardic
 */
public class JsonArrayUtils {

    public static JSONArray of(String json) {
        return new JSONArray(json);
    }

    public static JSONArray of(Object... items) {
        return new JSONArray(items);
    }

    public static JSONArray of(Collection<?> items) {
        return new JSONArray(items);
    }

    public static JSONArray of(Iterable<?> iterable) {
        return new JSONArray(iterable);
    }

    public static JSONArray of(JSONArray jsonArray) {
        return new JSONArray(jsonArray);
    }

    public static JSONArray ofLines(String jsonl) {
        return new JSONArray("[" + jsonl.replace("\n", ",") + "]");
    }

    public static List<Object> toList(String json) {
        JSONArray jsonArray = new JSONArray(json);

        return jsonArray.toList();
    }
}
