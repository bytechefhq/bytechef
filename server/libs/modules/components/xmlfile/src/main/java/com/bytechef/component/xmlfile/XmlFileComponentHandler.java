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

package com.bytechef.component.xmlfile;

import static com.bytechef.component.xmlfile.constants.XmlFileConstants.IS_ARRAY;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.READ;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.SOURCE;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.WRITE;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.any;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.showWhen;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.commons.xml.XmlUtils;
import com.bytechef.component.xmlfile.constants.XmlFileConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class XmlFileComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(XmlFileConstants.XML_FILE)
            .display(display("XML File").description("Reads and writes data from a XML file."))
            .actions(
                    action(READ)
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
                                    string(XmlFileConstants.PATH)
                                            .label("Path")
                                            .description(
                                                    "The path where the array is e.g 'data'. Leave blank to use the top level object.")
                                            .displayOption(showWhen(IS_ARRAY).eq(true)),
                                    integer(XmlFileConstants.PAGE_SIZE)
                                            .label("Page Size")
                                            .description("The amount of child elements to return in a page.")
                                            .displayOption(showWhen(IS_ARRAY).eq(true)),
                                    integer(XmlFileConstants.PAGE_NUMBER)
                                            .label("Page Number")
                                            .description("The page number to get.")
                                            .displayOption(showWhen(IS_ARRAY).eq(true)))
                            .output(
                                    array().displayOption(showWhen(IS_ARRAY).eq(true)),
                                    object().displayOption(showWhen(IS_ARRAY).eq(false)))
                            .perform(this::performRead),
                    action(WRITE)
                            .display(display("Write to file").description("Writes the data to a XML file."))
                            .properties(
                                    any(SOURCE)
                                            .label("Source")
                                            .description("The data to write to the file.")
                                            .required(true)
                                            .types(array(), object()),
                                    string(FILENAME)
                                            .label("Filename")
                                            .description(
                                                    "Filename to set for binary data. By default, \"file.xml\" will be used.")
                                            .required(true)
                                            .defaultValue("file.xml"))
                            .output(fileEntry())
                            .perform(this::performWrite));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    @SuppressWarnings("unchecked")
    protected Object performRead(Context context, ExecutionParameters executionParameters) {
        FileEntry fileEntry = executionParameters.getFileEntry(FILE_ENTRY);
        boolean isArray = executionParameters.getBoolean(IS_ARRAY, true);
        Object result;

        if (isArray) {
            String path = executionParameters.getString(XmlFileConstants.PATH);
            InputStream inputStream = context.getFileStream(fileEntry);
            List<Map<String, ?>> items;

            if (path == null) {
                try (Stream<Map<String, ?>> stream = XmlUtils.stream(context.getFileStream(fileEntry))) {
                    items = stream.toList();
                }
            } else {
                items = XmlUtils.read(inputStream, path, new TypeReference<>() {});
            }

            Integer pageSize = executionParameters.getInteger(XmlFileConstants.PAGE_SIZE);
            Integer pageNumber = executionParameters.getInteger(XmlFileConstants.PAGE_NUMBER);
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

    protected FileEntry performWrite(Context context, ExecutionParameters executionParameters) {
        Object source = executionParameters.getRequiredObject(SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            printWriter.println(XmlUtils.write(source));
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return context.storeFileContent(
                    executionParameters.getString(FILENAME) == null
                            ? "file.xml"
                            : executionParameters.getString(FILENAME),
                    inputStream);
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, ioException);
        }
    }
}
