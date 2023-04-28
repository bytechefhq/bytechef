
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
import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.XmlUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILENAME;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.SOURCE;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class XmlFileWriteAction {

    public static final ActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to a XML file.")
        .properties(
            oneOf(SOURCE)
                .label("Source")
                .description("The data to write to the file.")
                .required(true)
                .types(array(), object()),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.xml\" will be used.")
                .required(true)
                .defaultValue("file.xml")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .execute(XmlFileWriteAction::executeWrite);

    protected static FileEntry executeWrite(Context context, InputParameters inputParameters) {
        Object source = inputParameters.getRequired(SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            printWriter.println(XmlUtils.write(source));
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return context.storeFileContent(
                inputParameters.getString(FILENAME) == null
                    ? "file.xml"
                    : inputParameters.getString(FILENAME),
                inputStream);
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to handle action " + inputParameters, ioException);
        }
    }
}
