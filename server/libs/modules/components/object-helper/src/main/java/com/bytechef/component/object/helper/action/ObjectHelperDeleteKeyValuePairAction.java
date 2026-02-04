/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.object.helper.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.KEY;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kristián Stutiak
 */
public class ObjectHelperDeleteKeyValuePairAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteKeyValuePair")
        .title("Delete Key-Value Pair")
        .description(
            "Deletes a key-value pair in the given object by the specified key. Returns the modified object.")
        .properties(
            object(SOURCE)
                .label("Source")
                .description("The object from which to delete the key-value pair.")
                .required(true),
            string(KEY)
                .label("Key")
                .description("The key of the key-value pair to delete.")
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/object-helper#delete-key-value-pair")
        .perform(ObjectHelperDeleteKeyValuePairAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, Object> input = inputParameters.getRequiredMap(SOURCE, Object.class);
        String keyToDelete = inputParameters.getRequiredString(KEY);

        Map<String, Object> mutableMap = new HashMap<>(input);

        mutableMap.remove(keyToDelete);

        return mutableMap;
    }
}
