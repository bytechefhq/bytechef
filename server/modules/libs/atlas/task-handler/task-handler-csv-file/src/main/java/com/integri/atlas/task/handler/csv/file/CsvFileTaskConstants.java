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

package com.integri.atlas.task.handler.csv.file;

/**
 * @author Ivica Cardic
 */
public class CsvFileTaskConstants {

    public static final String PROPERTY_FILE_ENTRY = "fileEntry";
    public static final String PROPERTY_ROWS = "rows";
    public static final String PROPERTY_DELIMITER = "delimiter";
    public static final String PROPERTY_FILE_NAME = "fileName";
    public static final String PROPERTY_HEADER_ROW = "headerRow";
    public static final String PROPERTY_PAGE_SIZE = "pageSize";
    public static final String PROPERTY_PAGE_NUMBER = "pageNumber";
    public static final String PROPERTY_READ_AS_STRING = "readAsString";
    public static final String PROPERTY_INCLUDE_EMPTY_CELLS = "includeEmptyCells";
    public static final String TASK_CSV_FILE = "csvFile";

    enum Operation {
        READ,
        WRITE,
    }
}
