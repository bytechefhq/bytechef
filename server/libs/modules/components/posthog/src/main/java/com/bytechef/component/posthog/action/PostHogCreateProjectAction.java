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

package com.bytechef.component.posthog.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.posthog.constant.PostHogConstants.ID;
import static com.bytechef.component.posthog.constant.PostHogConstants.NAME;
import static com.bytechef.component.posthog.constant.PostHogConstants.PROJECT_OUTPUT_SCHEMA;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.posthog.util.PostHogUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PostHogCreateProjectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create Project")
        .description("Create a new Project.")
        .properties(
            string(ID)
                .label("Organization ID")
                .options((OptionsFunction<String>) PostHogUtils::getOrganizationOptions)
                .required(true),
            string(NAME)
                .label("Project Name")
                .required(true))
        .output(outputSchema(PROJECT_OUTPUT_SCHEMA))
        .perform(PostHogCreateProjectAction::perform);

    private PostHogCreateProjectAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post(
            "/api/organizations/%s/projects".formatted(inputParameters.getRequiredString(ID))))
            .body(
                Body.of(
                    Map.of(NAME, inputParameters.getRequiredString(NAME))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
