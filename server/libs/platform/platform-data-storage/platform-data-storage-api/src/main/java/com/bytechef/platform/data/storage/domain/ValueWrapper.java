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

package com.bytechef.platform.data.storage.domain;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;

/**
 * @author Ivica Cardic
 */
public record ValueWrapper(Object value, String classname) {

    public ValueWrapper(Object value) {
        this(value, getValueClass(value).getName());
    }

    public String write() {
        try {
            return JsonUtils.write(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ValueWrapper read(String json) {
        try {
            ValueWrapper valueWrapper = JsonUtils.read(json, ValueWrapper.class);

            return new ValueWrapper(
                ConvertUtils.convertValue(valueWrapper.value(), Class.forName(valueWrapper.classname())),
                valueWrapper.classname());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> getValueClass(Object value) {
        return value.getClass();
    }
}
