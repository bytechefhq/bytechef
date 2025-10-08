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

package com.bytechef.component.bitbucket.action;

import static com.bytechef.component.bitbucket.constant.BitbucketConstants.WORKSPACE;
import static com.bytechef.component.bitbucket.util.BitbucketUtils.getPaginationList;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.bitbucket.property.BitbucketProjectObjectProperties;
import com.bytechef.component.bitbucket.util.BitbucketUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BitbucketListProjectsAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listProjects")
        .title("List Projects")
        .description("Returns list of projects from workspace.")
        .properties(
            string(WORKSPACE)
                .label("Workspace")
                .description("Workspace from which projects are to be listed.")
                .required(true)
                .options((OptionsFunction<String>) BitbucketUtils::getWorkspaceOptions))
        .output(
            outputSchema(
                array()
                    .label("Projects")
                    .description("List of Bitbucket projects returned from the API.")
                    .items(
                        object()
                            .label("Project")
                            .description("Bitbucket project.")
                            .properties(
                                BitbucketProjectObjectProperties.PROPERTIES))))
        .perform(BitbucketListProjectsAction::perform);

    private BitbucketListProjectsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return getPaginationList(
            context, "/workspaces/%s/projects".formatted(inputParameters.getRequiredString(WORKSPACE)));
    }
}
