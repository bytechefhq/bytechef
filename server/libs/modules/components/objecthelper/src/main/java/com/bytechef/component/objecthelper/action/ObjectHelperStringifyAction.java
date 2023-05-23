
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

package com.bytechef.component.objecthelper.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.hermes.component.util.MapValueUtils;

import java.util.Map;

import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.STRINGIFY;
import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.TYPE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperStringifyAction {

    public static final ActionDefinition ACTION_DEFINITION = action(STRINGIFY)
        .title("Convert to JSON string")
        .description("Writes the object/array to a JSON string.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(SOURCE)
                .label("Source")
                .description("The data to convert to JSON string.")
                .displayCondition("type === 1")
                .required(true),
            array(SOURCE)
                .label("Source")
                .description("The data to convert to JSON string.")
                .displayCondition("type === 2")
                .required(true))
        .outputSchema(string())
        .execute(ObjectHelperStringifyAction::executeStringify);

    protected static String executeStringify(Context context, Map<String, ?> inputParameters) {
        Object input = MapValueUtils.getRequired(inputParameters, SOURCE);

        return JsonUtils.write(input);
    }
}
