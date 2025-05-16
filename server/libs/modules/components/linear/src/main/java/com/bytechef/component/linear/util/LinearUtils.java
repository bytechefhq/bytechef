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

package com.bytechef.component.linear.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.linear.constant.LinearConstants.ID;
import static com.bytechef.component.linear.constant.LinearConstants.ISSUE_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class LinearUtils {

    private LinearUtils() {
    }

    public static List<Option<String>> getAssigneeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "{users{nodes{id displayName}}}";

        Map<String, Object> result = executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (result.get("data") instanceof Map<?, ?> data &&
            data.get("users") instanceof Map<?, ?> users &&
            users.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("displayName"), (String) nodeMap.get("id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getIssueOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "{issues(filter: {team: {id: {eq: \"%s\" }}}){nodes{id title}}} "
            .formatted(inputParameters.getRequiredString(TEAM_ID));

        Map<String, Object> result = executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (result.get("data") instanceof Map<?, ?> data &&
            data.get("issues") instanceof Map<?, ?> issues &&
            issues.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("title"), (String) nodeMap.get("id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getProjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "{projects{nodes{id name}}}";

        Map<String, Object> result = executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (result.get("data") instanceof Map<?, ?> data &&
            data.get("projects") instanceof Map<?, ?> projects &&
            projects.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("name"), (String) nodeMap.get("id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getProjectStateOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "{projectStatuses {nodes {id name}}}";

        Map<String, Object> result = executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (result.get("data") instanceof Map<?, ?> data &&
            data.get("projectStatuses") instanceof Map<?, ?> projectStatuses &&
            projectStatuses.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("name"), (String) nodeMap.get("id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getIssueStateOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "{workflowStates{nodes{id name}}}";

        Map<String, Object> result = executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (result.get("data") instanceof Map<?, ?> data &&
            data.get("workflowStates") instanceof Map<?, ?> workflowStates &&
            workflowStates.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("name"), (String) nodeMap.get("id")));
                }
            }
        }
        return options;
    }

    public static List<Option<String>> getTeamOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "{teams{nodes{id name}}}";

        Map<String, Object> result = executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (result.get("data") instanceof Map<?, ?> data &&
            data.get("teams") instanceof Map<?, ?> teams &&
            teams.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("name"), (String) nodeMap.get("id")));
                }
            }
        }
        return options;
    }

    public static Map<String, Object> executeGraphQLQuery(String query, Context context) {
        return context
            .http(http -> http.post("/graphql"))
            .body(Body.of(Map.of("query", query)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static Map<String, Object>
        executeIssueTriggerQuery(String requiredAction, WebhookBody body, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});
        String action = (String) content.get("action");

        if (requiredAction.equals(action)) {
            Map<String, Object> data = (Map<String, Object>) content.get("data");
            String issueId = (String) data.get(ISSUE_ID);

            String query =
                "{issue(id: \"%s\") {id title team {id name} state {name} priority assignee {id name} description}}"
                    .formatted(issueId);

            Map<String, Object> response = executeGraphQLQuery(query, context);

            if (response.get("data") instanceof Map<?, ?> responseData &&
                responseData.get("issue") instanceof Map<?, ?> issue) {

                return (Map<String, Object>) issue;
            }
        }
        return null;
    }

    public static WebhookEnableOutput createWebhook(
        String type, String webhookUrl, TriggerContext context) {

        String query = "mutation {webhookCreate(input: {url: \"%s\",resourceTypes: [\"%s\"]}) {webhook {id}}}"
            .formatted(webhookUrl, type);

        Map<String, Object> body = executeGraphQLQuery(query, context);

        if (body.get("data") instanceof Map<?, ?> map && map.get("webhookCreate") instanceof Map<?, ?> webhookCreate
            && webhookCreate.get("webhook") instanceof Map<?, ?> webhook) {
            return new WebhookEnableOutput(Map.of(ID, webhook.get(ID)), null);
        }

        throw new ProviderException("Failed to start Linear webhook.");
    }

    public static void deleteWebhook(Parameters outputParameters, Context context) {
        String query = "mutation{webhookDelete(id: \"%s\")}".formatted(outputParameters.getString(ID));

        executeGraphQLQuery(query, context);
    }
}
