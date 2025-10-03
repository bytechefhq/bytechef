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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DESCRIPTION;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointCreateListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createList")
        .title("Create List")
        .description("Creates a new list")
        .properties(
            SITE_ID_PROPERTY,
            string(DISPLAY_NAME)
                .label("List Name")
                .required(true),
            string(DESCRIPTION)
                .label("List Description")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        dateTime("createdDateTime")
                            .description("The date and time when the list was created."),
                        string(DESCRIPTION)
                            .description("Description of the list."),
                        string("eTag"),
                        string(ID)
                            .description("ID of the list."),
                        dateTime("lastModifiedDateTime")
                            .description("The date and time when the list was last modified."),
                        string(NAME)
                            .description("Name of the list."),
                        string("webUrl")
                            .description("URL to access the list in a web browser."),
                        string(DISPLAY_NAME)
                            .description("The displayable title of the list."),
                        object("createdBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string(ID)
                                            .description("ID of the user who created the list."),
                                        string(DISPLAY_NAME)
                                            .description("Display name of the user who created the list."))),
                        object("list")
                            .properties(
                                bool("contentTypesEnabled")
                                    .description("Specifies whether content types are enabled for this list."),
                                bool("hidden")
                                    .description(
                                        "Specifies whether this list is hidden in the SharePoint user interface."),
                                string("template")))))
        .perform(MicrosoftSharePointCreateListAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointCreateListAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("/sites/" + inputParameters.getRequiredString(SITE_ID) + "/lists"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME),
                    DESCRIPTION, inputParameters.getRequiredString(DESCRIPTION)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
