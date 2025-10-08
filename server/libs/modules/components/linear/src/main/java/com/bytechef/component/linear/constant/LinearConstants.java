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

package com.bytechef.component.linear.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.linear.util.LinearUtils;

/**
 * @author Marija Horvat
 */
public class LinearConstants {

    public static final String ALL_PUBLIC_TEAMS = "allPublicTeams";
    public static final String ASSIGNEE_ID = "assigneeId";
    public static final String BODY = "body";
    public static final String DATA = "data";
    public static final String DESCRIPTION = "description";
    public static final String ID = "id";
    public static final String ISSUE_ID = "issueId";
    public static final String NAME = "name";
    public static final String NODES = "nodes";
    public static final String PRIORITY = "priority";
    public static final String PROJECT_ID = "projectId";
    public static final String QUERY = "query";
    public static final String START_DATE = "startDate";
    public static final String STATUS_ID = "statusId";
    public static final String TEAM_ID = "teamId";
    public static final String TITLE = "title";
    public static final String VARIABLES = "variables";

    public static final ModifiableBooleanProperty ALL_PUBLIC_TEAMS_PROPERTY = bool(ALL_PUBLIC_TEAMS)
        .label("All Public Teams")
        .description("If true, the webhook will be created for all public teams.")
        .defaultValue(false)
        .required(true);

    public static final ModifiableStringProperty TEAM_ID_TRIGGER_PROPERTY = string(TEAM_ID)
        .label("Team ID")
        .displayCondition("%s == false".formatted(ALL_PUBLIC_TEAMS))
        .options((OptionsFunction<String>) LinearUtils::getTeamOptions)
        .required(true);

    public static final ModifiableObjectProperty TRIGGER_OUTPUT_PROPERTY =
        object()
            .properties(
                string(ID)
                    .description("The unique identifier of the entity."),
                string(TITLE)
                    .description("The issue's title."),
                object("team")
                    .description("The team that the issue is associated with.")
                    .properties(
                        string(ID)
                            .description("The unique identifier of the entity."),
                        string(NAME)
                            .description("The team's name.")),
                object("state")
                    .description("The workflow state that the issue is associated with.")
                    .properties(
                        string(NAME)
                            .description("The state's name.")),
                string(PRIORITY)
                    .description("The priority of the issue."),
                object("assignee")
                    .description("The user to whom the issue is assigned to.")
                    .properties(
                        string(ID)
                            .description("The unique identifier of the entity."),
                        string(NAME)
                            .description("The user's full name.")),
                string(DESCRIPTION)
                    .description("The issue's description in markdown format."));

    private LinearConstants() {
    }
}
