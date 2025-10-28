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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GithubNewIssueTrigger {

    public static final String LAST_TIME_CHECKED = "lastTimeChecked";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newIssue")
        .title("New Issue")
        .description("Triggers when a new issue is created.")
        .type(TriggerType.POLLING)
        .properties(
            string(REPOSITORY)
                .label("Repository")
                .options((OptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .required(true))
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .poll(GithubNewIssueTrigger::poll);

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(zoneId));

        List<Map<String, ?>> issues = context.http(
            http -> http.get(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/issues"))
            .queryParameter("since", formattedStartDate)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new PollOutput(issues, Map.of(LAST_TIME_CHECKED, now), false);
    }

    private GithubNewIssueTrigger() {
    }
}
