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

package com.bytechef.component.object.helper.action;

import static com.bytechef.component.definition.ComponentDsl.*;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.object.helper.constant.ObjectHelperConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kristián Stutiak
 */
public class ObjectHelperDeleteKeyValuePairAction {

    // Definition of the action to delete a key-value pair from an object.
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION =
        action(ObjectHelperConstants.DELETE_KEY_VALUE_PAIR)
            .title("Delete Key-Value Pair")
            .description(
                "Deletes a key-value pair in the given object by the specified key. Returns the modified object.")
            .properties(object(ObjectHelperConstants.INPUT) // Input object from which to delete the key-value pair
                .label("Input")
                .description("The object from which to delete the key-value pair.")
                .required(true),
                string(ObjectHelperConstants.KEY) // Key of the pair to be deleted
                    .label("Key")
                    .description("The key of the key-value pair to delete.")
                    .required(true))
            .output() // Specifies that the output will be an object
            .perform(ObjectHelperDeleteKeyValuePairAction::perform); // Sets the method to perform the action

    // Method that performs the action of deleting the key-value pair
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object inputObject = inputParameters.getRequired(ObjectHelperConstants.INPUT); // Retrieve the input object
        String keyToDelete = (String) inputParameters.getRequired(ObjectHelperConstants.KEY); // Cast the key to a
                                                                                              // String

        // Cast the input object to a Map
        Map<String, Object> inputMap = (Map<String, Object>) inputObject;

        // Create a mutable copy of the input map to allow modification
        Map<String, Object> mutableMap = new HashMap<>(inputMap);

        // Remove the specified key from the mutable map
        mutableMap.remove(keyToDelete);

        return mutableMap; // Return the modified map
    }

}
