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

package com.bytechef.component.github.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.github.action.GithubStarRepositoryAction.PROPERTIES;
import static com.bytechef.component.github.constant.GithubConstants.STAR_REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.STAR_REPOSITORY_DESCRIPTION;
import static com.bytechef.component.github.constant.GithubConstants.STAR_REPOSITORY_TITLE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.github.action.GithubStarRepositoryAction;

/**
 * @author Monika Kušter
 */
public class GithubStarRepositoryTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(STAR_REPOSITORY)
            .title(STAR_REPOSITORY_TITLE)
            .description(STAR_REPOSITORY_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .object(() -> GithubStarRepositoryAction::perform);
}
