/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

public class Json {

    private static final ObjectMapper defaultObjectMapper = new ObjectMapper();

    public static <T> T deserialize(ObjectMapper aObjectMapper, String aValue, Class<T> aClass) {
        try {
            return aObjectMapper.readValue(aValue, aClass);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static String serialize(Object aValue) {
        return serialize(defaultObjectMapper, aValue);
    }

    public static String serialize(ObjectMapper aObjectMapper, Object aValue) {
        try {
            return aObjectMapper.writeValueAsString(aValue);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
