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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.BASE_URL;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.CREATE_LIST;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DESCRIPTION;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointCreateListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_LIST)
        .title("Create list")
        .description("Creates a new list")
        .properties(
            SITE_ID_PROPERTY,
            string(DISPLAY_NAME)
                .label("List name")
                .required(true),
            string(DESCRIPTION)
                .label("List description")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string(DESCRIPTION),
                    string(DISPLAY_NAME)))
        .perform(MicrosoftSharePointCreateListAction::perform);

    private MicrosoftSharePointCreateListAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(BASE_URL + "/" + inputParameters.getRequiredString(SITE_ID) + "/lists"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME),
                    DESCRIPTION, inputParameters.getRequiredString(DESCRIPTION)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
