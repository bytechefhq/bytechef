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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.vercel.constant.VercelConstants.ORG;
import static com.bytechef.component.vercel.constant.VercelConstants.PROJECT;
import static com.bytechef.component.vercel.constant.VercelConstants.REF;
import static com.bytechef.component.vercel.constant.VercelConstants.REPO;
import static com.bytechef.component.vercel.constant.VercelConstants.SHA;
import static com.bytechef.component.vercel.constant.VercelConstants.TARGET;
import static com.bytechef.component.vercel.constant.VercelConstants.TEAM_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.vercel.util.VercelUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class VercelCreateDeploymentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createDeployment")
        .title("Create Deployment")
        .description("Creates a deployment of a Git branch or commit in a Vercel project.")
        .properties(
            string(PROJECT)
                .label("Project")
                .description("The Vercel project to deploy.")
                .options((OptionsFunction<String>) VercelUtils::getProjectIdOptions)
                .required(true),
            string(ORG)
                .label("Repository Owner")
                .description("The GitHub organization or user that owns the repository.")
                .required(true),
            string(REPO)
                .label("Repository")
                .description("The name of the GitHub repository.")
                .required(true),
            string(REF)
                .label("Git Ref")
                .description("The branch or tag to deploy.")
                .required(true),
            string(SHA)
                .label("Commit SHA")
                .description("The commit to deploy. Leave empty to deploy the latest commit of the ref.")
                .required(false),
            string(TARGET)
                .label("Target")
                .description("The environment to deploy to. A preview deployment is created when not production.")
                .options(
                    option("Preview", "preview"),
                    option("Production", "production"))
                .defaultValue("preview")
                .required(false),
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
        .perform(VercelCreateDeploymentAction::perform);

    private VercelCreateDeploymentAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String project = inputParameters.getRequiredString(PROJECT);

        Map<String, Object> gitSource = new HashMap<>();

        gitSource.put("type", "github");
        gitSource.put("org", inputParameters.getRequiredString(ORG));
        gitSource.put("repo", inputParameters.getRequiredString(REPO));
        gitSource.put("ref", inputParameters.getRequiredString(REF));

        String sha = inputParameters.getString(SHA);

        if (sha != null && !sha.isBlank()) {
            gitSource.put("sha", sha);
        }

        Map<String, Object> body = new HashMap<>();

        body.put("name", project);
        body.put("project", project);
        body.put("gitSource", gitSource);

        if ("production".equals(inputParameters.getString(TARGET))) {
            body.put("target", "production");
        }

        Executor executor = context.http(http -> http.post("/v13/deployments"));

        executor = VercelUtils.withTeamId(executor, inputParameters.getString(TEAM_ID));

        Map<String, Object> response = executor
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return VercelUtils.toDeploymentSummary(response);
    }
}
