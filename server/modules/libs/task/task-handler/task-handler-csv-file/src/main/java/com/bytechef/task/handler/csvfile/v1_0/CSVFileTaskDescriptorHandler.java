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

package com.bytechef.task.handler.csvfile.v1_0;

import static com.bytechef.hermes.descriptor.model.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.BOOLEAN_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.DATE_TIME_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.FILE_ENTRY_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.INTEGER_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.NUMBER_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.model.DSL.OPTIONS;
import static com.bytechef.hermes.descriptor.model.DSL.STRING_PROPERTY;
import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_ENTRY;
import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_NAME;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.AGE_NUMBER;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.CSV_FILE;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.DELIMITER;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.HEADER_ROW;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.PAGE_SIZE;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.READ_AS_STRING;
import static com.bytechef.task.handler.csvfile.CSVFileTaskConstants.ROWS;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.model.DSL;
import com.bytechef.hermes.descriptor.model.TaskDescriptor;
import com.bytechef.task.handler.csvfile.CSVFileTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class CSVFileTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(CSV_FILE)
            .displayName("CSV File")
            .description("Reads and writes data from a csv file.")
            .version(CSVFileTaskConstants.VERSION)
            .operations(
                    OPERATION(CSVFileTaskConstants.READ)
                            .displayName("Read from file")
                            .description("Reads data from a csv file.")
                            .inputs(
                                    FILE_ENTRY_PROPERTY(FILE_ENTRY)
                                            .displayName("File")
                                            .description(
                                                    "The object property which contains a reference to the csv file to read from.")
                                            .required(true),
                                    OPTIONS()
                                            .displayName("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    STRING_PROPERTY(DELIMITER)
                                                            .displayName("Delimiter")
                                                            .description("Delimiter to use when reading a csv file.")
                                                            .defaultValue(","),
                                                    BOOLEAN_PROPERTY(HEADER_ROW)
                                                            .displayName("Header Row")
                                                            .description(
                                                                    "The first row of the file contains the header names.")
                                                            .defaultValue(true),
                                                    BOOLEAN_PROPERTY(INCLUDE_EMPTY_CELLS)
                                                            .displayName("Include Empty Cells")
                                                            .description(
                                                                    "When reading from file the empty cells will be filled with an empty string.")
                                                            .defaultValue(false),
                                                    INTEGER_PROPERTY(PAGE_SIZE)
                                                            .displayName("Page Size")
                                                            .description(
                                                                    "The amount of child elements to return in a page."),
                                                    INTEGER_PROPERTY(AGE_NUMBER)
                                                            .displayName("Page Number")
                                                            .description("The page number to get."),
                                                    BOOLEAN_PROPERTY(READ_AS_STRING)
                                                            .displayName("Read As String")
                                                            .description(
                                                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.")
                                                            .defaultValue(false)))
                            .outputs(ARRAY_PROPERTY()),
                    OPERATION(CSVFileTaskConstants.WRITE)
                            .displayName("Write to file")
                            .description("Writes the data to a csv file.")
                            .inputs(
                                    ARRAY_PROPERTY(ROWS)
                                            .displayName("Rows")
                                            .description("The array of objects to write to the file.")
                                            .required(true)
                                            .items(OBJECT_PROPERTY()
                                                    .additionalProperties(true)
                                                    .properties(
                                                            BOOLEAN_PROPERTY(),
                                                            DATE_TIME_PROPERTY(),
                                                            NUMBER_PROPERTY(),
                                                            STRING_PROPERTY())),
                                    OPTIONS()
                                            .displayName("Options")
                                            .placeholder("Add Option")
                                            .options(STRING_PROPERTY(FILE_NAME)
                                                    .displayName("File Name")
                                                    .description(
                                                            "File name to set for binary data. By default, \"file.csv\" will be used.")
                                                    .defaultValue("")))
                            .outputs(FILE_ENTRY_PROPERTY()));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
