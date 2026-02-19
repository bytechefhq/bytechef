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

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.google.api.services.gmail.model.Label;
import java.util.List;

/**
 * @author Nikolina Špehar
 */
public class GoogleMailListLabelsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listLabels")
        .title("List Labels")
        .description("Lists labels that are connected to your Google Mail account.")
        .help("", "https://docs.bytechef.io/reference/components/google-mail_v1#list-labels")
        .output(
            outputSchema(
                array()
                    .description("List of all labels in the user's mailbox.")
                    .items(
                        object()
                            .description("Label object containing 'name' and 'id'.")
                            .properties(
                                string("name")
                                    .description("The display name of the label."),
                                string("id")
                                    .description("ID of the label."),
                                string("messageListVisibility")
                                    .description(
                                        "The visibility of messages with this label in the message list in the Gmail " +
                                            "web interface."),
                                string("labelListVisibility")
                                    .description(
                                        "The visibility of the label in the label list in the Gmail web interface."),
                                string("type")
                                    .description("The owner type for the label.")))))
        .perform(GoogleMailListLabelsAction::perform);

    private GoogleMailListLabelsAction() {
    }

    public static List<Label> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return GoogleMailUtils.getLabels(connectionParameters);
    }
}
