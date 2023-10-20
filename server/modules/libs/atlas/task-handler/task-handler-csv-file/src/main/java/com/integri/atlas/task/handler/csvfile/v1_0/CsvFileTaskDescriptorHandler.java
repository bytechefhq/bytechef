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

package com.integri.atlas.task.handler.csvfile.v1_0;

import static com.integri.atlas.task.descriptor.model.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.DATE_TIME_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.NUMBER_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.OPERATION;
import static com.integri.atlas.task.descriptor.model.DSL.OPTIONS;
import static com.integri.atlas.task.descriptor.model.DSL.STRING_PROPERTY;

import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.model.DSL;
import com.integri.atlas.task.descriptor.model.TaskDescriptor;
import com.integri.atlas.task.handler.csvfile.CsvFileTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class CsvFileTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL
        .createTaskDescriptor(CsvFileTaskConstants.CSV_FILE)
        .displayName("CSV File")
        .description("Reads and writes data from a csv file.")
        .version(CsvFileTaskConstants.VERSION)
        .operations(
            OPERATION(CsvFileTaskConstants.READ)
                .displayName("Read from file")
                .description("Reads data from a csv file.")
                .inputs(
                    FILE_ENTRY_PROPERTY(CsvFileTaskConstants.FILE_ENTRY)
                        .displayName("File")
                        .description("The object property which contains a reference to the csv file to read from.")
                        .required(true),
                    OPTIONS()
                        .displayName("Options")
                        .placeholder("Add Option")
                        .options(
                            STRING_PROPERTY(CsvFileTaskConstants.DELIMITER)
                                .displayName("Delimiter")
                                .description("Delimiter to use when reading a csv file.")
                                .defaultValue(","),
                            BOOLEAN_PROPERTY(CsvFileTaskConstants.HEADER_ROW)
                                .displayName("Header Row")
                                .description("The first row of the file contains the header names.")
                                .defaultValue(true),
                            BOOLEAN_PROPERTY(CsvFileTaskConstants.INCLUDE_EMPTY_CELLS)
                                .displayName("Include Empty Cells")
                                .description(
                                    "When reading from file the empty cells will be filled with an empty string."
                                )
                                .defaultValue(false),
                            INTEGER_PROPERTY(CsvFileTaskConstants.PAGE_SIZE)
                                .displayName("Page Size")
                                .description("The amount of child elements to return in a page."),
                            INTEGER_PROPERTY(CsvFileTaskConstants.AGE_NUMBER)
                                .displayName("Page Number")
                                .description("The page number to get."),
                            BOOLEAN_PROPERTY(CsvFileTaskConstants.READ_AS_STRING)
                                .displayName("Read As String")
                                .description(
                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way."
                                )
                                .defaultValue(false)
                        )
                )
                .outputs(ARRAY_PROPERTY()),
            OPERATION(CsvFileTaskConstants.WRITE)
                .displayName("Write to file")
                .description("Writes the data to a csv file.")
                .inputs(
                    ARRAY_PROPERTY(CsvFileTaskConstants.ROWS)
                        .displayName("Rows")
                        .description("The array of objects to write to the file.")
                        .required(true)
                        .items(
                            OBJECT_PROPERTY()
                                .additionalProperties(true)
                                .properties(
                                    BOOLEAN_PROPERTY(),
                                    DATE_TIME_PROPERTY(),
                                    NUMBER_PROPERTY(),
                                    STRING_PROPERTY()
                                )
                        ),
                    OPTIONS()
                        .displayName("Options")
                        .placeholder("Add Option")
                        .options(
                            STRING_PROPERTY(CsvFileTaskConstants.FILE_NAME)
                                .displayName("File Name")
                                .description("File name to set for binary data. By default, \"file.csv\" will be used.")
                                .defaultValue("")
                        )
                )
                .outputs(FILE_ENTRY_PROPERTY())
        );

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
