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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.INPUTS;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REF;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.WORKFLOW;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.github.util.GithubUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubTriggerWorkflowDispatchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("triggerWorkflowDispatch")
        .title("Trigger Workflow Dispatch")
        .description(
            "Starts a GitHub Actions workflow that has a workflow_dispatch trigger. With a classic OAuth token the " +
                "connection needs the repo scope; a fine-grained token needs the Actions: write permission.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#trigger-workflow-dispatch")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository that contains the workflow.")
                .required(true),
            string(WORKFLOW)
                .label("Workflow")
                .description("The workflow file name, for example build.yml, or the workflow ID.")
                .options((OptionsFunction<String>) GithubUtils::getWorkflowOptions)
                .optionsLookupDependsOn(REPOSITORY, OWNER)
                .required(true),
            string(REF)
                .label("Ref")
                .description(
                    "The branch or tag to run the workflow on. Defaults to the repository's default branch.")
                .required(false),
            object(INPUTS)
                .label("Inputs")
                .description(
                    "Values for the workflow's workflow_dispatch inputs, keyed by input name. GitHub accepts at " +
                        "most 25 inputs.")
                .additionalProperties(string())
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(WORKFLOW)
                            .description("The workflow that was dispatched."),
                        string(REF)
                            .description("The branch or tag the workflow runs on."),
                        bool("dispatched")
                            .description("True when GitHub accepted the dispatch."))))
        .perform(GithubTriggerWorkflowDispatchAction::perform);

    private GithubTriggerWorkflowDispatchAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String owner = inputParameters.getRequiredString(OWNER);
        String repository = inputParameters.getRequiredString(REPOSITORY);
        String workflow = inputParameters.getRequiredString(WORKFLOW);

        String ref = inputParameters.getString(REF);

        if (ref == null || ref.isBlank()) {
            ref = GithubUtils.getDefaultBranch(owner, repository, context);
        }

        String dispatchRef = ref;

        context
            .http(http -> http.post(
                "/repos/%s/%s/actions/workflows/%s/dispatches".formatted(owner, repository, workflow)))
            .body(
                Body.of(
                    REF, dispatchRef,
                    INPUTS, inputParameters.getMap(INPUTS, Object.class, Map.of())))
            .configuration(responseType(ResponseType.JSON))
            .execute();

        return Map.of(WORKFLOW, workflow, REF, dispatchRef, "dispatched", true);
    }
}
