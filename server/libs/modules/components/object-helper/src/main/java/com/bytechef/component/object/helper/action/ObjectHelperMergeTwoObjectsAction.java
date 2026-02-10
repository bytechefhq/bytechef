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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TARGET;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;

/**
 * @author Monika Kušter
 */
public class ObjectHelperMergeTwoObjectsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("mergeTwoObjects")
        .title("Merge Two Objects")
        .description(
            "Merge two objects into one. If there is any property with the same name, the source value will be used.")
        .properties(
            object(SOURCE)
                .label("Source")
                .description("The source object to merge.")
                .required(true),
            object(TARGET)
                .label("Target")
                .description("The target object to merge into.")
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/object-helper_v1#merge-two-objects")
        .perform(ObjectHelperMergeTwoObjectsAction::perform);

    private ObjectHelperMergeTwoObjectsAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return new HashMap<String, Object>() {
            {
                putAll(inputParameters.getRequiredMap(TARGET, Object.class));
                putAll(inputParameters.getRequiredMap(SOURCE, Object.class));
            }
        };
    }
}
