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

package com.integri.atlas.json.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * @author Ivica Cardic
 */
public class JSONItem extends JSONObject {

    public static JSONItem of(String key, boolean value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, double value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, float value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, int value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, long value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, String value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, Map<?, ?> value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, Collection<?> value) {
        return (JSONItem) new JSONItem().put(key, value);
    }

    public static JSONItem of(String key, Object value) {
        return (JSONItem) new JSONItem().put(key, value);
    }
}
