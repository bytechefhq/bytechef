
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
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.MapValueUtils;
import com.bytechef.hermes.component.util.XmlUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILENAME;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.SOURCE;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.TYPE;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class XmlFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to a XML file.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(SOURCE)
                .label("Source")
                .description("The object to write to the file.")
                .displayCondition("type === 1")
                .required(true),
            array(SOURCE)
                .label("Source")
                .description("The aray to write to the file.")
                .displayCondition("type === 2")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Filename to set for binary data. By default, \"file.xml\" will be used.")
                .required(true)
                .defaultValue("file.xml")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .perform(XmlFileWriteAction::perform);

    protected static FileEntry perform(Map<String, ?> inputParameters, Context context) {
        Object source = MapValueUtils.getRequired(inputParameters, SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            printWriter.println(XmlUtils.write(source));
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return context.storeFileContent(
                MapValueUtils.getString(inputParameters, FILENAME, "file.xml"), inputStream);
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to handle action " + inputParameters, ioException);
        }
    }
}
