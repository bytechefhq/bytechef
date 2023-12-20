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

package com.bytechef.component.xml.helper.action;

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.xml.helper.constant.XmlHelperConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class XmlHelperStringifyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(XmlHelperConstants.STRINGIFY)
        .title("Convert to XML string")
        .description("Writes the object/array to a XML string.")
        .properties(
            integer(XmlHelperConstants.TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(XmlHelperConstants.SOURCE)
                .label("Source")
                .description("The object to convert to XML string.")
                .displayCondition("type === 1")
                .required(true),
            array(XmlHelperConstants.SOURCE)
                .label("Source")
                .description("The array to convert to XML string.")
                .displayCondition("type === 2")
                .required(true))
        .outputSchema(string())
        .perform(XmlHelperStringifyAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context.xml(xml -> xml.write(inputParameters.getRequired(XmlHelperConstants.SOURCE)));
    }
}
