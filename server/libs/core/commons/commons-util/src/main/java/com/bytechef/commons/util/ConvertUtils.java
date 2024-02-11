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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConvertUtils {

    @SuppressFBWarnings("MS_PKGPROTECT")
    protected static ObjectMapper objectMapper;

    public static boolean canConvert(Object fromValue, Class<?> toValueType) {
        try {
            objectMapper.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            // ignore
            return false;
        }

        return true;
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    public static <T> T convertValue(Object fromValue, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.convertValue(fromValue, typeFactory.constructType(type));
    }

    @Autowired
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    void setObjectMapper(ObjectMapper objectMapper) {
        ConvertUtils.objectMapper = objectMapper;
    }
}
