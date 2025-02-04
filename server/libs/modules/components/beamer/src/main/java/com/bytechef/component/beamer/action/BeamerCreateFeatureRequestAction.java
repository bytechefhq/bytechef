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

package com.bytechef.component.beamer.action;

import static com.bytechef.component.beamer.constant.BeamerConstants.CONTENT;
import static com.bytechef.component.beamer.constant.BeamerConstants.FEATURE_REQUEST_OUTPUT;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_EMAIL;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BeamerCreateFeatureRequestAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createFeatureRequest")
        .title("Create Feature Request")
        .description("Create a new Feature Request")
        .properties(
            string(TITLE)
                .label("Feature Request Title")
                .description("The name of the new feature request")
                .required(true),
            string(CONTENT)
                .label("Feature Request Content")
                .description("The content of the new feature request")
                .required(false),
            string(USER_EMAIL)
                .label("User Email")
                .description("The email of the user that is creating the new feature request")
                .required(false))
        .output(outputSchema(FEATURE_REQUEST_OUTPUT))
        .perform(BeamerCreateFeatureRequestAction::perform);

    private BeamerCreateFeatureRequestAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(
                "/requests"))
            .body(
                Body.of(
                    TITLE, List.of(inputParameters.getRequiredString(TITLE)),
                    CONTENT, List.of(inputParameters.getString(CONTENT)),
                    USER_EMAIL, inputParameters.getString(USER_EMAIL)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
