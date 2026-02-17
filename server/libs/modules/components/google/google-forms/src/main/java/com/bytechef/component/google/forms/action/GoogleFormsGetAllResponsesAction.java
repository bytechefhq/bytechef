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
import static com.bytechef.component.google.forms.util.GoogleFormsUtils.getCustomResponses;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleFormsGetAllResponsesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getAllResponses")
        .title("Get All Responses")
        .description("Get all responses of a form.")
        .help("", "https://docs.bytechef.io/reference/components/google-forms_v1#get-all-responses")
        .properties(
            string(FORM_ID)
                .label("Form ID")
                .description("ID of the form whose responses to retrieve.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_FORM, true))
                .required(true))
        .output()
        .perform(GoogleFormsGetAllResponsesAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleFormsGetAllResponsesAction() {
    }

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return getCustomResponses(actionContext, inputParameters.getRequiredString(FORM_ID), null);
    }
}
