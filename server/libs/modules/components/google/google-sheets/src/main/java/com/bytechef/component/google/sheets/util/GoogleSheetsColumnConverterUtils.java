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

package com.bytechef.component.google.sheets.util;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsColumnConverterUtils {

    /**
     * Returns column name in <code>A,B,C,..,AA,AB</code> naming convention.
     *
     * @param columnNumber column order number in column sequence
     * @return column name in <code>column_A</code> format
     */
    public static String columnToLabel(int columnNumber) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            int modulo = (columnNumber - 1) % 26;
            columnName.insert(0, (char) (65 + modulo));
            columnNumber = (columnNumber - modulo) / 26;
        }

        return columnName.toString();
    }

    public static Integer labelToColumn(String label) {
        int columnNumber = 0;

        for (int i = 0; i < label.length(); i++) {
            columnNumber = columnNumber * 26 + label.charAt(i) - 'A' + 1;
        }

        return columnNumber;
    }

    private GoogleSheetsColumnConverterUtils() {
    }
}
