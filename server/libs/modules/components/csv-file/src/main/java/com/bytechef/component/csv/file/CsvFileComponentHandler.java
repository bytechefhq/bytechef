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

package com.bytechef.component.csv.file;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.csv.file.action.CsvFileReadAction;
import com.bytechef.component.csv.file.action.CsvFileWriteAction;
import com.bytechef.component.csv.file.constant.CsvFileConstants;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DataStreamItemReader;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class CsvFileComponentHandler implements ComponentHandler {

    public static final ComponentDefinition COMPONENT_DEFINITION = component(CsvFileConstants.CSV_FILE)
        .title("CSV File")
        .description("Reads and writes data from a csv file.")
        .icon("path:assets/csv-file.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(CsvFileReadAction.ACTION_DEFINITION, CsvFileWriteAction.ACTION_DEFINITION)
        .dataStreamItemReader(new DataStreamItemReader() {});

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
