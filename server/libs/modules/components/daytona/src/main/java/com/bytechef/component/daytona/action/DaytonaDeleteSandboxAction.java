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

package com.bytechef.component.daytona.action;

import static com.bytechef.component.daytona.constant.DaytonaConstants.SANDBOX_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.daytona.util.DaytonaUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * Deletes a persistent Daytona sandbox previously created with {@code createSandbox} (or kept alive by
 * {@code executeCode}), freeing its resources.
 *
 * @author Ivica Cardic
 */
public class DaytonaDeleteSandboxAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteSandbox")
        .title("Delete Sandbox")
        .description("Deletes a Daytona sandbox and frees its resources.")
        .properties(
            string(SANDBOX_ID)
                .label("Sandbox ID")
                .description("The ID of the sandbox to delete.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("deleted")
                            .description("Whether the sandbox was deleted."))))
        .perform(DaytonaDeleteSandboxAction::perform);

    private DaytonaDeleteSandboxAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        boolean deleted = DaytonaUtils.deleteSandbox(context, inputParameters.getRequiredString(SANDBOX_ID));

        return Map.of("deleted", deleted);
    }
}
