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

package com.bytechef.component.xml.file.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.FileEntry;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.xml.file.constant.XmlFileConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivica Cardic
 */
public class XmlFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(XmlFileConstants.WRITE)
        .title("Write to file")
        .description("Writes the data to a XML file.")
        .properties(
            integer(XmlFileConstants.TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(XmlFileConstants.SOURCE)
                .label("Source")
                .description("The object to write to the file.")
                .displayCondition("type === 1")
                .required(true),
            array(XmlFileConstants.SOURCE)
                .label("Source")
                .description("The aray to write to the file.")
                .displayCondition("type === 2")
                .required(true),
            string(XmlFileConstants.FILENAME)
                .label("Filename")
                .description("Filename to set for binary data. By default, \"file.xml\" will be used.")
                .required(true)
                .defaultValue("file.xml")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .perform(XmlFileWriteAction::perform);

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        Object source = inputParameters.getRequired(XmlFileConstants.SOURCE);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            printWriter.println((String) context.xml(xml -> xml.write(source)));
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return context.file(file -> file.storeContent(
                inputParameters.getString(XmlFileConstants.FILENAME, "file.xml"), inputStream));
        }
    }
}
