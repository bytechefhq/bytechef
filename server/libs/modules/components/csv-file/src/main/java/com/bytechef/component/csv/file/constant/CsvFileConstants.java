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

package com.bytechef.component.csv.file.constant;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class CsvFileConstants {

    public static final String ROWS = "rows";
    public static final String DELIMITER = "delimiter";
    public static final String ENCLOSING_CHARACTER = "enclosingCharacter";
    public static final String HEADER_ROW = "headerRow";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String READ_AS_STRING = "readAsString";
    public static final String INCLUDE_EMPTY_CELLS = "includeEmptyCells";
    public static final String CSV_FILE = "csvFile";
    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String FILENAME = "filename";

    public static final CsvMapper CSV_MAPPER = new CsvMapper();
}
