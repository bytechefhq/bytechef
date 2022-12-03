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

package com.bytechef.hermes.component.utils;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ValueUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValueUtils.class);

    public static Boolean booleanOf(String string) {
        return BooleanUtils.toBooleanObject(string);
    }

    public static Double doubleOf(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Integer intOf(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Long longOf(String string) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Object valueOF(String string) {
        Object value = intOf(string);

        if (value == null) {
            value = longOf(string);
        }

        if (value == null) {
            value = doubleOf(string);
        }

        if (value == null) {
            value = booleanOf(string);
        }

        if (value == null) {
            value = string;
        }

        return value;
    }
}
