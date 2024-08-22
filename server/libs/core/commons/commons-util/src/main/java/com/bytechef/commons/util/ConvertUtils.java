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

package com.bytechef.commons.util;

import static com.bytechef.commons.util.constant.ObjectMapperConstants.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.type.TypeFactory;
import java.lang.reflect.Type;

/**
 * @author Ivica Cardic
 */
public class ConvertUtils {

    public static boolean canConvert(Object fromValue, Class<?> toValueType) {
        try {
            OBJECT_MAPPER.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            // ignore
            return false;
        }

        return true;
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return OBJECT_MAPPER.convertValue(fromValue, toValueType);
    }

    public static <T> T convertValue(Object fromValue, Type type) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return OBJECT_MAPPER.convertValue(fromValue, typeFactory.constructType(type));
    }
}
