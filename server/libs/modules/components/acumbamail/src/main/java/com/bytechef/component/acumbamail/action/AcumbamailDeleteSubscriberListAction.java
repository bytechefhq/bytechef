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

package com.bytechef.component.acumbamail.action;

import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.LIST_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.acumbamail.util.AcumbamailUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class AcumbamailDeleteSubscriberListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteSubscriberList")
        .title("Delete Subscriber List")
        .description("Deletes a list of subscribers.")
        .properties(
            integer(LIST_ID)
                .label("List Id")
                .description("List identifier.")
                .options((OptionsFunction<String>) AcumbamailUtils::getListsIdOptions)
                .required(true))
        .perform(AcumbamailDeleteSubscriberListAction::perform);

    private AcumbamailDeleteSubscriberListAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/deleteList/"))
            .queryParameter(LIST_ID, inputParameters.getRequiredString(LIST_ID))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
