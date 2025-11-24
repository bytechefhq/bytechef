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

package com.bytechef.component.github.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.PULL_REQUESTS;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.util.GithubUtils.getItems;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.github.util.GithubUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GithubNewPullRequestTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPullRequest")
        .title("New Pull Request")
        .description("Triggers when a new pull request is created.")
        .type(TriggerType.POLLING)
        .properties(
            string(REPOSITORY)
                .label("Repository")
                .options((OptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .required(true))
        .output()
        .poll(GithubNewPullRequestTrigger::poll);

    private GithubNewPullRequestTrigger() {
    }

    public static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        boolean editorEnvironment = context.isEditorEnvironment();
        String url = "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/pulls";

        List<Map<String, ?>> pullRequests = getItems(
            context, url, editorEnvironment, "sort", "created", "direction", "desc");

        List<Long> pullRequestIds = new ArrayList<>(pullRequests.size());

        for (Map<String, ?> pullRequest : pullRequests) {
            pullRequestIds.add((Long) pullRequest.get(ID));
        }

        if (editorEnvironment) {
            return new PollOutput(pullRequests, Map.of(PULL_REQUESTS, pullRequestIds), false);
        }

        List<Long> previousPullRequestIds = closureParameters.getList(PULL_REQUESTS, Long.class);

        if (previousPullRequestIds == null) {
            return new PollOutput(List.of(), Map.of(PULL_REQUESTS, pullRequestIds), false);
        }

        List<Map<String, ?>> newPullRequests = new ArrayList<>();

        for (Map<String, ?> pullRequest : pullRequests) {
            Long id = (Long) pullRequest.get(ID);

            if (!previousPullRequestIds.contains(id)) {
                newPullRequests.add(pullRequest);
            }
        }

        return new PollOutput(newPullRequests, Map.of(PULL_REQUESTS, pullRequestIds), false);
    }
}
