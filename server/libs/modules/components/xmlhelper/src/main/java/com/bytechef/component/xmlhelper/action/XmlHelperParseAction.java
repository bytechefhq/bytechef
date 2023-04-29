
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

package com.bytechef.component.xmlhelper.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.util.XmlUtils;

import java.util.Map;

import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.PARSE;
import static com.bytechef.component.xmlhelper.constant.XmlHelperConstants.SOURCE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class XmlHelperParseAction {

    public static final ActionDefinition ACTION_DEFINITION = action(PARSE)
        .title("Convert from XML string")
        .description("Converts the XML string to object/array.")
        .properties(string(SOURCE)
            .label("Source")
            .description("The XML string to convert to the data.")
            .required(true))
        .outputSchema(object())
        .execute(XmlHelperParseAction::executeParse);

    protected static Map<String, ?> executeParse(Context context, InputParameters inputParameters) {
        String source = inputParameters.getRequiredString(SOURCE);

        return XmlUtils.read(source);
    }
}
