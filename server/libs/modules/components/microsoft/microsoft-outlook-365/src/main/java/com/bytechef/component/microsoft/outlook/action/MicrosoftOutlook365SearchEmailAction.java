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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SEARCH_EMAIL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.microsoft.graph.requests.MessageCollectionPage;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365SearchEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH_EMAIL)
        .title("Search Email")
        .description("Get the messages in the signed-in user's mailbox")
        .properties(
        // TODO
        )
        .outputSchema(
            // TODO
            string())
        .perform(MicrosoftOutlook365SearchEmailAction::perform);

    private MicrosoftOutlook365SearchEmailAction() {
    }

    public static MessageCollectionPage perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO

        return null;

    }
}
