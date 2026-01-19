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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Špehar
 */
public class GoogleMailListLabelsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listLabels")
        .title("List Labels")
        .description("Lists labels that are connected to your Google Mail account.")
        .output(
            outputSchema(
                array()
                    .description("List of labels. Each label is represented as an object with 'name' and 'id'.")
                    .items(
                        object()
                            .description("Label object containing 'name' and 'id'.")
                            .properties(
                                string("name")
                                    .description("Name of the label."),
                                string("id")
                                    .description("Id of the label.")))))
        .perform(GoogleMailListLabelsAction::perform);

    private GoogleMailListLabelsAction() {
    }

    public static List<Map<String, String>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Option<String>> labelOptions = GoogleMailUtils.getLabelOptions(
            inputParameters, connectionParameters, Map.of(), "", (ActionContext) context);

        List<Map<String, String>> labels = new ArrayList<>();

        labelOptions.forEach(labelOption -> {
            Map<String, String> label = Map.of(
                NAME, labelOption.getLabel(),
                ID, labelOption.getValue());

            labels.add(label);
        });

        return labels;
    }
}
