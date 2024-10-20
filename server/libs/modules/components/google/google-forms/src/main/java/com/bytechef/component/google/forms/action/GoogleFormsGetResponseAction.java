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

import static com.bytechef.component.definition.ComponentDsl.*;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE;

import com.bytechef.component.definition.*;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;

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
                .options((OptionsDataSource.ActionOptionsFunction<String>) GoogleFormsUtils::getFormOptions)
                .required(true),
            string(RESPONSE)
                .label("Response")
                .description("Response to retrieve.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) GoogleFormsUtils::getResponseOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("formId"),
                        string("responseId"),
                        string("createTime"),
                        string("lastSubmittedTime"),
                        string("respondentEmail"),
                        object("answers"), // map of question - answer - dynamic schema so cannot be defined here
                        number("totalScore"))))
        .perform(GoogleFormsGetResponseAction::perform);

    private GoogleFormsGetResponseAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        String formId = inputParameters.getRequiredString(FORM);
        String responseId = inputParameters.getRequiredString(RESPONSE);

        return actionContext
            .http(http -> http.get("https://forms.googleapis.com/v1/forms/" + formId + "/responses/" + responseId))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
