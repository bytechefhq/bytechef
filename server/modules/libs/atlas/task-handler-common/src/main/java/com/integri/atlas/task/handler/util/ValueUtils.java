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

package com.integri.atlas.task.handler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ValueUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValueUtils.class);

    public static Boolean getBoolean(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        return null;
    }

    public static Double getDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Integer getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Long getLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Object getValueFromString(String valueString) {
        Object value = getInt(valueString);

        if (value == null) {
            value = getLong(valueString);
        }

        if (value == null) {
            value = getDouble(valueString);
        }

        if (value == null) {
            value = getBoolean(valueString);
        }

        if (value == null) {
            value = valueString;
        }

        return value;
    }
}
