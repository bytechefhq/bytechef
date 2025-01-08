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

package com.bytechef.component.google.forms.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE;
import static com.bytechef.component.google.forms.util.GoogleFormsUtils.getCustomResponse;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import java.util.Map;

/**
 * @author Vihar Shah
 */
public class GoogleFormsGetResponseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getResponse")
        .title("Get Response")
        .description("Get the response of a form.")
        .properties(
            string(FORM)
                .label("Form")
                .description("Form to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleFormsUtils::getFormOptions)
                .required(true),
            string(RESPONSE)
                .label("Response")
                .description("Response to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleFormsUtils::getResponseOptions)
                .optionsLookupDependsOn(FORM)
                .required(true))
        .output()
        .perform(GoogleFormsGetResponseAction::perform);

    private GoogleFormsGetResponseAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String formId = inputParameters.getRequiredString(FORM);
        String responseId = inputParameters.getRequiredString(RESPONSE);

        Map<String, Object> response = actionContext
            .http(http -> http.get("https://forms.googleapis.com/v1/forms/" + formId + "/responses/" + responseId))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getCustomResponse(actionContext, formId, response);
    }
}
