/*
 * Copyright 2025 ByteChef
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class ConvertUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConvertUtils.class);

    private static ObjectMapper objectMapper;

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

    public static <T> T convertValue(Object fromValue, Class<T> toValueType, boolean includeNulls) {
        ObjectMapper currentObjectMapper = objectMapper;

        if (includeNulls) {
            currentObjectMapper = currentObjectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.ALWAYS);
        }

        return currentObjectMapper.convertValue(fromValue, toValueType);
    }

    public static <T> T convertValue(Object fromValue, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.convertValue(fromValue, typeFactory.constructType(type));
    }

    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return objectMapper.convertValue(fromValue, toValueTypeRef);
    }

    public static Object convertString(String str) {
        if (str == null) {
            return null;
        }

        String trimmedString = str.trim();

        Object value = null;

        for (Function<String, Object> transformerFunction : parseFunctions) {
            try {
                value = transformerFunction.apply(trimmedString);
            } catch (NumberFormatException | DateTimeParseException exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }

                continue;
            }

            if (value != null) {
                return value;
            }
        }

        if (trimmedString.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }

        if (trimmedString.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }

        return str;
    }

    private static final List<Function<String, Object>> parseFunctions;

    static {
        parseFunctions = List.of(
            Integer::parseInt, Long::parseLong, Double::parseDouble, LocalDateTime::parse, LocalDate::parse);
    }

    @SuppressFBWarnings("EI")
    public static void setObjectMapper(ObjectMapper objectMapper) {
        ConvertUtils.objectMapper = objectMapper;
    }
}
