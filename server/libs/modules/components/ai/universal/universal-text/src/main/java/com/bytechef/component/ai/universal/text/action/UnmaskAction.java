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

package com.bytechef.component.ai.universal.text.action;

import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MASK_MAP;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
public final class UnmaskAction {

    private UnmaskAction() {
    }

    public static ModifiableActionDefinition of() {
        return action(AiTextConstants.UNMASK)
            .title("Unmask")
            .description("Restores the tokens in a text from the map the Mask action returned.")
            .properties(
                string(TEXT)
                    .label("Text")
                    .description("The text to process.")
                    .required(true),
                object(MASK_MAP)
                    .label("Masked map")
                    .description("Map of tokens to the values to restore.")
                    .additionalProperties(string()))
            .output(
                outputSchema(string().description("The text with tokens restored.")),
                sampleOutput("Hello, my name is John Doe and my email is john@example.com."))
            .help("", "https://docs.bytechef.io/reference/components/ai-text_v1#unmask")
            .perform(UnmaskAction::perform);
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String text = inputParameters.getRequiredString(TEXT);
        Map<String, String> maskMap = inputParameters.getMap(MASK_MAP, String.class, Map.of());

        for (Map.Entry<String, String> entry : maskMap.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        return text;
    }
}
