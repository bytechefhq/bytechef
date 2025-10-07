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

package com.bytechef.component.google.forms.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.APPLICATION_VND_GOOGLE_APPS_FORM;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE_ID;
import static com.bytechef.component.google.forms.util.GoogleFormsUtils.createCustomResponse;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Vihar Shah
 */
public class GoogleFormsGetResponseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getResponse")
        .title("Get Response")
        .description("Get the response of a form.")
        .properties(
            string(FORM_ID)
                .label("Form ID")
                .description("ID of the form whose response to retrieve.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FORM, true))
                .required(true),
            string(RESPONSE_ID)
                .label("Response ID")
                .description("ID of the response to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleFormsUtils::getResponseIdOptions)
                .optionsLookupDependsOn(FORM_ID)
                .required(true))
        .output()
        .perform(GoogleFormsGetResponseAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleFormsGetResponseAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String formId = inputParameters.getRequiredString(FORM_ID);
        String responseId = inputParameters.getRequiredString(RESPONSE_ID);

        Map<String, Object> response = actionContext
            .http(http -> http.get("https://forms.googleapis.com/v1/forms/" + formId + "/responses/" + responseId))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return createCustomResponse(actionContext, formId, response);
    }
}
