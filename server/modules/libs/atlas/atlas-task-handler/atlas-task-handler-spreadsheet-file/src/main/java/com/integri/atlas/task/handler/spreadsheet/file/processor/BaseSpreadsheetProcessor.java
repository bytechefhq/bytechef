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

package com.integri.atlas.task.handler.spreadsheet.file.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public abstract class BaseSpreadsheetProcessor implements SpreadsheetProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BaseSpreadsheetProcessor.class);

    protected Object getBoolean(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        return null;
    }

    protected Object getDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    protected Object getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    protected Object getLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    protected Object getValueFromString(String valueString) {
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
