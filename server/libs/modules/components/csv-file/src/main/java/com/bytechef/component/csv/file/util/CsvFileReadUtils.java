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

package com.bytechef.component.csv.file.util;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.CSV_MAPPER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ENCLOSING_CHARACTER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_NUMBER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.READ_AS_STRING;

import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public class CsvFileReadUtils {

    public static Map<String, Object> getColumnRow(
        ReadConfiguration configuration, List<?> row, char enclosingCharacter) {

        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < row.size(); i++) {
            map.put(
                "column_" + (i + 1),
                processValue(
                    (String) row.get(i), enclosingCharacter, configuration.includeEmptyCells(),
                    configuration.readAsString()));
        }

        return map;
    }

    public static char getEnclosingCharacter(ReadConfiguration configuration) {
        char enclosingCharacter = (char) -1;

        String configurationEnclosingCharacter = configuration.enclosingCharacter();

        if (!StringUtils.isEmpty(configurationEnclosingCharacter)) {
            enclosingCharacter = configurationEnclosingCharacter.charAt(0);
        }

        return enclosingCharacter;
    }

    public static Map<String, Object> getHeaderRow(
        ReadConfiguration configuration, Map<?, ?> row, char enclosingCharacter) {

        Map<String, Object> map = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : row.entrySet()) {
            map.put(
                strip((String) entry.getKey(), enclosingCharacter),
                processValue(
                    (String) entry.getValue(), enclosingCharacter, configuration.includeEmptyCells(),
                    configuration.readAsString()));
        }

        return map;
    }

    public static MappingIterator<Object> getIterator(
        BufferedReader bufferedReader, ReadConfiguration configuration) throws IOException {

        MappingIterator<Object> iterator;

        if (configuration.headerRow()) {
            String delimiter = configuration.delimiter();

            CsvSchema headerSchema = CsvSchema
                .emptySchema()
                .withHeader()
                .withColumnSeparator(delimiter.charAt(0));

            iterator = CSV_MAPPER
                .readerForMapOf(String.class)
                .with(headerSchema)
                .readValues(bufferedReader);
        } else {
            iterator = CSV_MAPPER
                .readerForListOf(String.class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(bufferedReader);
        }

        return iterator;
    }

    public static ReadConfiguration getReadConfiguration(Parameters inputParameters) {
        String delimiter = inputParameters.getString(DELIMITER, ",");
        String enclosingCharacter = inputParameters.getString(ENCLOSING_CHARACTER, "");
        boolean headerRow = inputParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = inputParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageNumber = inputParameters.getInteger(PAGE_NUMBER);
        Integer pageSize = inputParameters.getInteger(PAGE_SIZE);
        boolean readAsString = inputParameters.getBoolean(READ_AS_STRING, false);

        Integer rangeStartRow = null;
        Integer rangeEndRow = null;

        if (pageSize != null && pageNumber != null) {
            rangeStartRow = pageSize * pageNumber - pageSize;

            rangeEndRow = rangeStartRow + pageSize;
        }

        return new ReadConfiguration(
            delimiter, enclosingCharacter, headerRow, includeEmptyCells,
            rangeStartRow == null ? 0 : rangeStartRow,
            rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow, readAsString);
    }

    public static Object processValue(
        String valueString, char enclosingCharacter, boolean includeEmptyCells, boolean readAsString) {

        Object value = null;

        if (valueString == null || valueString.isEmpty()) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (enclosingCharacter != (char) -1) {
                valueString = strip(valueString, enclosingCharacter);
            }

            if (readAsString) {
                value = valueString;
            } else {
                value = valueOf(valueString);
            }
        }

        return value;
    }

    public static String strip(String valueString, char enclosingCharacter) {
        valueString = valueString.strip();

        valueString = StringUtils.removeStart(valueString, enclosingCharacter);

        return StringUtils.removeEnd(valueString, String.valueOf(enclosingCharacter));
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static Object valueOf(String string) {
        Object value = null;

        try {
            value = Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            // ignore
        }

        if (value == null) {
            try {
                value = Long.parseLong(string);
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }

        if (value == null) {
            try {
                value = Double.parseDouble(string);
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }

        if (value == null) {
            value = BooleanUtils.toBooleanObject(string);
        }

        if (value == null) {
            value = string;
        }

        return value;
    }
}
