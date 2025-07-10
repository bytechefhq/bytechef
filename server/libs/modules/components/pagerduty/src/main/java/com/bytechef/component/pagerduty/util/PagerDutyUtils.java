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

package com.bytechef.component.pagerduty.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNEE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNMENTS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.BODY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.DETAILS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ESCALATION_POLICY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ID;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_KEY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.NAME;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.PRIORITY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.SERVICE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TITLE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.URGENCY;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PagerDutyUtils {

    public static List<Option<String>> getEscalationPolicyIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getIdOptions(context, "escalation_policies", NAME);
    }

    public static List<Option<String>> getIncidentIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getIdOptions(context, "incidents", TITLE);
    }

    public static List<Option<String>> getIncidentTypeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> response = context.http(http -> http.get("/incidents/types"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> incidentType : response.get("incident_types")) {
            options.add(option((String) incidentType.get("display_name"), (String) incidentType.get(NAME)));
        }

        return options;
    }

    public static List<Option<String>> getPriorityOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getIdOptions(context, "priorities", NAME);
    }

    public static Map<String, Object> getRequestBody(Parameters parameters) {
        Map<String, Object> body = new HashMap<>();

        body.put(TYPE, INCIDENT);

        addIfNotNull(SERVICE, parameters.getString(SERVICE), body);
        addIfNotNull(PRIORITY, parameters.getString(PRIORITY), body);
        addIfNotNull(ESCALATION_POLICY, parameters.getString(ESCALATION_POLICY), body);
        addIfNotNull(TITLE, parameters.getString(TITLE), body);
        addIfNotNull(URGENCY, parameters.getString(URGENCY), body);
        addIfNotNull(INCIDENT_KEY, parameters.getString(INCIDENT_KEY), body);
        addIfNotNull(DETAILS, parameters.getString(DETAILS), body);
        addIfNotNull(INCIDENT_TYPE, parameters.getString(INCIDENT_TYPE), body);

        List<String> assignments = parameters.getList(ASSIGNMENTS, String.class);
        List<Object> assignees = new ArrayList<>();

        if (assignments != null) {
            for (String assignment : assignments) {
                assignees.add(
                    Map.of(ASSIGNEE, Map.of(ID, assignment, TYPE, "user_reference")));
            }

            body.put(ASSIGNMENTS, assignees);
        }

        return body;
    }

    public static List<Option<String>> getServiceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getIdOptions(context, "services", NAME);
    }

    public static List<Option<String>> getUserIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getIdOptions(context, "users", NAME);
    }

    private static void addIfNotNull(String key, String value, Map<String, Object> map) {
        if (value != null) {
            switch (key) {
                case DETAILS -> map.put(BODY, Map.of(TYPE, "incident_body", key, value));
                case ESCALATION_POLICY, PRIORITY, SERVICE ->
                    map.put(key, Map.of(ID, value, TYPE, "%s_reference".formatted(key)));
                case INCIDENT_KEY, TITLE, URGENCY -> map.put(key, value);
                case INCIDENT_TYPE -> map.put(key, Map.of(NAME, value));
                default -> {
                }
            }
        }
    }

    private static List<Option<String>> getIdOptions(Context context, String url, String label) {
        List<Option<String>> idOptions = new ArrayList<>();

        Map<String, Object> response = context.http(http -> http.get("/" + url))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get(url) instanceof List<?> options) {
            for (Object optionsObject : options) {
                if (optionsObject instanceof Map<?, ?> option) {
                    idOptions.add(option((String) option.get(label), (String) option.get(ID)));
                }
            }
        }

        return idOptions;
    }
}
