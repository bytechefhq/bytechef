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

package com.bytechef.component.vercel.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.vercel.constant.VercelConstants.DEPLOYMENT_ID;
import static com.bytechef.component.vercel.constant.VercelConstants.TEAM_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.vercel.util.VercelUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class VercelGetDeploymentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDeployment")
        .title("Get Deployment")
        .description("Retrieves a Vercel deployment with its state and, when it failed, the error reported by Vercel.")
        .properties(
            string(DEPLOYMENT_ID)
                .label("Deployment ID")
                .description("The ID of the deployment.")
                .required(true),
            string(TEAM_ID)
                .label("Team ID")
                .description("The team to act on behalf of. Leave empty to use the personal account.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id"),
                        string("url"),
                        string("state"),
                        string("errorCode"),
                        string("errorMessage"))))
        .perform(VercelGetDeploymentAction::perform);

    private VercelGetDeploymentAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String deploymentId = inputParameters.getRequiredString(DEPLOYMENT_ID);

        Executor executor =
            context.http(http -> http.get("/v13/deployments/" + VercelUtils.encodePathSegment(deploymentId)));

        executor = VercelUtils.withTeamId(executor, inputParameters.getString(TEAM_ID));

        Map<String, Object> response = executor
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return VercelUtils.toDeploymentSummary(response);
    }
}
