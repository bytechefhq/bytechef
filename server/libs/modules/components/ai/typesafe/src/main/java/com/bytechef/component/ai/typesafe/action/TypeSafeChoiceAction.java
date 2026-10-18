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

package com.bytechef.component.ai.typesafe.action;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CHOICE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CONFIDENCE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CRITERIA;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.DESCRIPTION;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.LABEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_OUTPUT_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.OPTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.PROBABILITIES;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.TYPE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.USAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.ai.typesafe.util.TypeSafeUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks exactly one label from a fixed set of options.
 *
 * @author Ivica Cardic
 */
public class TypeSafeChoiceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CHOICE)
        .title("Choice")
        .description(
            "Pick exactly one label from a fixed set of options. Returns the selected label, a probability for " +
                "every option and a confidence. A choice always names a winner, even when no option fits; ask a " +
                "separate Noul if \"none of these\" is a real outcome.")
        .properties(
            MODEL_PROPERTY,
            STATE_PROPERTY,
            string(INSTRUCTIONS)
                .label("Instructions")
                .description("The question to answer by picking an option, e.g. \"Which team should handle this?\"")
                .controlType(TEXT_AREA)
                .required(true),
            array(OPTIONS)
                .label("Options")
                .description("The labels to choose from. Labels must be unique.")
                .items(
                    object()
                        .properties(
                            string(LABEL)
                                .label("Label")
                                .description("The value returned when this option is chosen, e.g. `billing`.")
                                .required(true),
                            string(DESCRIPTION)
                                .label("Description")
                                .description("What this option covers, e.g. \"Payments, invoicing, refunds\".")
                                .required(false)))
                .minItems(2)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(TYPE)
                            .description("The answer type, always `choice`."),
                        string(CHOICE)
                            .description("The label of the selected option."),
                        object(PROBABILITIES)
                            .description("The probability of every option, keyed by label; they sum to one.")
                            .additionalProperties(number()),
                        number(CONFIDENCE)
                            .description("How concentrated the probabilities are on the selected option, 0 to 1."),
                        MODEL_OUTPUT_PROPERTY,
                        USAGE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/typesafe_v1#choice")
        .perform(TypeSafeChoiceAction::perform);

    private TypeSafeChoiceAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> options = inputParameters.getRequiredList(OPTIONS, new TypeReference<>() {});

        Map<String, Object> criteria = new LinkedHashMap<>();

        for (Map<String, Object> option : options) {
            Object label = option.get(LABEL);

            if (!(label instanceof String labelText) || labelText.isBlank()) {
                throw new IllegalArgumentException("Every option needs a label");
            }

            if (criteria.containsKey(labelText)) {
                throw new IllegalArgumentException("Option labels must be unique, found '%s' twice".formatted(label));
            }

            Object description = option.get(DESCRIPTION);

            criteria.put(
                labelText, description instanceof String descriptionText && !descriptionText.isBlank()
                    ? descriptionText : null);
        }

        Map<String, Object> question = new LinkedHashMap<>();

        question.put(TYPE, CHOICE);
        question.put(INSTRUCTIONS, inputParameters.getRequiredString(INSTRUCTIONS));
        question.put(CRITERIA, criteria);

        return TypeSafeUtils.askQuestion(inputParameters, question, context);
    }
}
