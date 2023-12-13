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

package com.bytechef.component.xmlfile.action;

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
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaResponse;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class XmlFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ)
        .title("Read from file")
        .description("Reads data from a XML file.")
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
                .displayCondition("%s === true".formatted(IS_ARRAY))
                .advancedOption(true),
            integer(PAGE_SIZE)
                .label("Page Size")
                .description("The amount of child elements to return in a page.")
                .displayCondition("%s === true".formatted(IS_ARRAY))
                .advancedOption(true),
            integer(PAGE_NUMBER)
                .label("Page Number")
                .description("The page number to get.")
                .displayCondition("%s === true".formatted(IS_ARRAY))
                .advancedOption(true))
        .outputSchema(getOutputSchemaFunction())
        .perform(XmlFileReadAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        FileEntry fileEntry = inputParameters.getRequired(FILE_ENTRY, FileEntry.class);
        boolean isArray = inputParameters.getBoolean(IS_ARRAY, true);
        Object result;

        if (isArray) {
            String path = inputParameters.getString(PATH);
            InputStream inputStream = context.file(file -> file.getStream(fileEntry));
            List<Map<String, ?>> items;

            if (path == null) {
                try (Stream<Map<String, ?>> stream = context.xml(
                    xml -> xml.stream(context.file(file -> file.getStream(fileEntry))))) {

                    items = stream.toList();
                }
            } else {
                items = context.xml(xml -> xml.readList(inputStream, path, new Context.TypeReference<>() {}));
            }

            Integer pageSize = inputParameters.getInteger(PAGE_SIZE);
            Integer pageNumber = inputParameters.getInteger(PAGE_NUMBER);
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
            result = context.xml(xml -> xml.read((String) context.file(file -> file.readToString(fileEntry))));
        }

        return result;
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters, connection, context) -> {
            if (inputParameters.getBoolean(IS_ARRAY, false)) {
                return new OutputSchemaResponse(object());
            } else {
                return new OutputSchemaResponse(array());
            }
        };
    }
}
