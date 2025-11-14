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

import static com.bytechef.component.csv.file.constant.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ENCLOSING_CHARACTER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_NUMBER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.READ_AS_STRING;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.Parameters;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
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

        int currColumn = 1;

        for (Map.Entry<?, ?> entry : row.entrySet()) {
            String strippedString = strip((String) entry.getKey(), enclosingCharacter);

            if (strippedString.isEmpty()) {
                strippedString = "column_" + currColumn;
            }

            map.put(
                strippedString,
                processValue(
                    (String) entry.getValue(), enclosingCharacter, configuration.includeEmptyCells(),
                    configuration.readAsString()));

            currColumn++;
        }

        return map;
    }

    public static Iterator<CSVRecord> getIterator(
        BufferedReader bufferedReader, ReadConfiguration configuration) throws IOException {

        class CSVHeaderBuilder {
            static String[] asArray(String headerRow, String delimiter) {
                List<String> regexReservedCharacters = Arrays.asList(".", "+", "*", "?", "^", "$", "(", ")", "[", "]",
                    "{", "}", "|",
                    "\\");

                String regexPrefix = "";
                if (regexReservedCharacters.contains(delimiter)) {
                    regexPrefix = "\\";
                }

                String[] originalHeaderRow = headerRow.split(regexPrefix + delimiter, -1);
                Map<String, Integer> repetitiveHeaderCounter = new HashMap<>();
                String[] usableHeaderRow = new String[originalHeaderRow.length];

                for (int i = 0; i < originalHeaderRow.length; i++) {
                    String header = originalHeaderRow[i];
                    if ("".equals(header)) {
                        header = "NULL";
                    }

                    if (repetitiveHeaderCounter.containsKey(header)) {
                        repetitiveHeaderCounter.put(header, repetitiveHeaderCounter.get(header) + 1);

                        usableHeaderRow[i] = String.format("%s{%d}", header, repetitiveHeaderCounter.get(header));
                    } else {
                        repetitiveHeaderCounter.put(header, 1);
                        usableHeaderRow[i] = header;
                    }

                }

                return usableHeaderRow;
            }
        }

        String delimiter = configuration.delimiter();
        String[] headerRow = null;

        if (configuration.headerRow()) {
            String headerString = bufferedReader.readLine();
            if (headerString != null) {
                headerRow = CSVHeaderBuilder.asArray(headerString, delimiter);
            } else {
                throw new NullPointerException();
            }

        }

        CSVFormat csvFormat = CSVFormat.Builder.create()
            .setIgnoreEmptyLines(false)
            .setDelimiter(delimiter)
            .setHeader(headerRow)
            .get();

        return csvFormat.parse(bufferedReader)
            .iterator();
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
                value = ConvertUtils.convertString(valueString);
            }
        }

        return value;
    }

    public static String strip(String valueString, char enclosingCharacter) {
        valueString = valueString.strip();

        valueString = StringUtils.removeStart(valueString, enclosingCharacter);

        return StringUtils.removeEnd(valueString, String.valueOf(enclosingCharacter));
    }

}
