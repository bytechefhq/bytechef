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

import static com.bytechef.component.daytona.constant.DaytonaConstants.LANGUAGE;
import static com.bytechef.component.daytona.constant.DaytonaConstants.SANDBOX_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.daytona.util.DaytonaUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * Creates a persistent Daytona sandbox and returns its id, so an agent can run multiple {@code executeCode} calls that
 * share filesystem and process state, then tear it down with {@code deleteSandbox}.
 *
 * @author Ivica Cardic
 */
public class DaytonaCreateSandboxAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSandbox")
        .title("Create Sandbox")
        .description(
            "Creates a persistent, isolated Daytona sandbox and returns its ID. Reuse the ID across executeCode " +
                "and uploadFile calls to preserve state, then delete it with deleteSandbox when done.")
        .properties(
            string(LANGUAGE)
                .label("Language")
                .description("The default language runtime for the sandbox.")
                .options(
                    option("Python", "python"),
                    option("TypeScript", "typescript"),
                    option("JavaScript", "javascript"),
                    option("Bash", "bash"))
                .defaultValue("python")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(SANDBOX_ID)
                            .description("The ID of the created sandbox."))))
        .perform(DaytonaCreateSandboxAction::perform);

    private DaytonaCreateSandboxAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String sandboxId = DaytonaUtils.createSandbox(context, inputParameters.getString(LANGUAGE));

        return Map.of(SANDBOX_ID, sandboxId);
    }
}
