
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

package com.bytechef.component.xmlfile.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.utils.XmlUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILE_ENTRY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.IS_ARRAY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PAGE_SIZE;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PATH;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.READ;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.show;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class XmlFileReadAction {

    public static final ActionDefinition ACTION_DEFINITION = action(READ)
        .display(display("Read from file").description("Reads data from a XML file."))
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the XML file to read from.")
                .required(true),
            bool(IS_ARRAY)
                .label("Is Array")
                .description("The object input is array?")
                .defaultValue(true),
            string(PATH)
                .label("Path")
                .description(
                    "The path where the array is e.g 'data'. Leave blank to use the top level object.")
                .displayOption(show(IS_ARRAY, true))
                .advancedOption(true),
            integer(PAGE_SIZE)
                .label("Page Size")
                .description("The amount of child elements to return in a page.")
                .displayOption(show(IS_ARRAY, true))
                .advancedOption(true),
            integer(PAGE_NUMBER)
                .label("Page Number")
                .description("The page number to get.")
                .displayOption(show(IS_ARRAY, true))
                .advancedOption(true))
        .output(
            array().displayOption(show(IS_ARRAY, true)),
            object().displayOption(show(IS_ARRAY, false)))
        .perform(XmlFileReadAction::performRead);

    public static Object performRead(Context context, ExecutionParameters executionParameters) {
        FileEntry fileEntry = executionParameters.get(FILE_ENTRY, FileEntry.class);
        boolean isArray = executionParameters.getBoolean(IS_ARRAY, true);
        Object result;

        if (isArray) {
            String path = executionParameters.getString(PATH);
            InputStream inputStream = context.getFileStream(fileEntry);
            List<Map<String, ?>> items;

            if (path == null) {
                try (Stream<Map<String, ?>> stream = XmlUtils.stream(context.getFileStream(fileEntry))) {
                    items = stream.toList();
                }
            } else {
                items = XmlUtils.read(inputStream, path, new TypeReference<>() {});
            }

            Integer pageSize = executionParameters.getInteger(PAGE_SIZE);
            Integer pageNumber = executionParameters.getInteger(PAGE_NUMBER);
            Integer rangeStartIndex = null;
            Integer rangeEndIndex = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartIndex = pageSize * pageNumber - pageSize;

                rangeEndIndex = rangeStartIndex + pageSize;
            }

            if (rangeStartIndex != null && rangeStartIndex > 0
                || rangeEndIndex != null && rangeEndIndex < items.size()) {
                items = items.subList(rangeStartIndex, rangeEndIndex);
            }

            result = items;
        } else {
            result = XmlUtils.read(context.readFileToString(fileEntry), Map.class);
        }

        return result;
    }
}
