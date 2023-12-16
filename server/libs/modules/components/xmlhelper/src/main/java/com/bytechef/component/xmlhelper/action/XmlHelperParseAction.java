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

package com.bytechef.component.xmlhelper.action;

import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.PARSE;
import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.SOURCE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class XmlHelperParseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(PARSE)
        .title("Convert from XML string")
        .description("Converts the XML string to object/array.")
        .properties(
            string(SOURCE)
                .label("Source")
                .description("The XML string to convert to the data.")
                .required(true))
        .outputSchema(object())
        .perform(XmlHelperParseAction::perform);

    protected static Map<String, ?> perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        return context.xml(xml -> xml.read(inputParameters.getRequiredString(SOURCE)));
    }
}
