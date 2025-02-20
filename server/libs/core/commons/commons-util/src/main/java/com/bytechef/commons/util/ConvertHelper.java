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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ConvertHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertHelper.class);

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public ConvertHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean canConvert(Object fromValue, Class<?> toValueType) {
        try {
            objectMapper.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            // ignore
            return false;
        }

        return true;
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType, boolean includeNulls) {
        ObjectMapper currentObjectMapper = objectMapper;

        if (includeNulls) {
            currentObjectMapper = currentObjectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.ALWAYS);
        }

        return currentObjectMapper.convertValue(fromValue, toValueType);
    }

    public <T> T convertValue(Object fromValue, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.convertValue(fromValue, typeFactory.constructType(type));
    }

    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return objectMapper.convertValue(fromValue, toValueTypeRef);
    }

    public Object convertString(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(str);
        }

        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        return str;
    }
}
