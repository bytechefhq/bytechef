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

package com.bytechef.component.random.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.UUID;

/**
 * @author Monika Kušter
 */
public class RandomHelperRandomUuidAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("randomUuid")
        .title("Random UUID")
        .description("Generates a random UUID.")
        .help("", "https://docs.bytechef.io/reference/components/random-helper_v1#random-uuid")
        .output(
            outputSchema(
                string()
                    .description("Generated UUID.")))
        .perform(RandomHelperRandomUuidAction::perform);

    private RandomHelperRandomUuidAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return String.valueOf(UUID.randomUUID());
    }
}
