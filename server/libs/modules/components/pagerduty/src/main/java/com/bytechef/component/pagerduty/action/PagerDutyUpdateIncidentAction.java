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

package com.bytechef.component.pagerduty.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNEE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ASSIGNMENTS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ESCALATION_POLICY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.FROM;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_ID;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_OBJECT;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.INCIDENT_TYPE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.PRIORITY;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.RESOLUTION;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.STATUS;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.TITLE;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.URGENCY;
import static com.bytechef.component.pagerduty.util.PagerDutyUtils.getRequestBody;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pagerduty.util.PagerDutyUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PagerDutyUpdateIncidentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateIncident")
        .title("Update Incident")
        .description("Update selected incident.")
        .properties(
            string(FROM)
                .label("From")
                .description("The email address of a valid user associated with the account making the request.")
                .required(true),
            string(INCIDENT_ID)
                .label("Incident ID")
                .description("ID of the incident to which the note will be added.")
                .options((OptionsFunction<String>) PagerDutyUtils::getIncidentIdOptions)
                .required(true),
            string(INCIDENT_TYPE)
                .label("Incident Type")
                .description("Incident type.")
                .options((OptionsFunction<String>) PagerDutyUtils::getIncidentTypeOptions)
                .required(true),
            string(STATUS)
                .label("Status")
                .description("The new status of the incident.")
                .options(
                    option("Resolved", "resolved"),
                    option("Acknowledged", "acknowledged"))
                .required(false),
            array(ASSIGNMENTS)
                .label("Assignments")
                .description("Assign the incident to these assignees.")
                .options((OptionsFunction<String>) PagerDutyUtils::getUserIdOptions)
                .items(
                    string(ASSIGNEE)
                        .label("Assignee")
                        .description("Incident will be assigned to this user.")
                        .required(false))
                .required(false),
            string(RESOLUTION)
                .label("Resolution")
                .description("The resolution for this incident.")
                .displayCondition("%s == '%s'".formatted(STATUS, "resolved"))
                .required(false),
            string(TITLE)
                .label("Title")
                .description("A short description of the nature, symptoms, cause, or effect of the incident.")
                .required(false),
            string(PRIORITY)
                .label("Priority")
                .description(
                    "Priority of the incident. Priorities must be enabled in your PagerDuty account in order to use " +
                        "them.")
                .options((OptionsFunction<String>) PagerDutyUtils::getPriorityOptions)
                .required(false),
            string(URGENCY)
                .label("Urgency")
                .description("The urgency level of this incident.")
                .options(
                    option("High", "high"),
                    option("Low", "low"))
                .required(false),
            string(ESCALATION_POLICY)
                .label("Escalation Policy")
                .description(
                    "Delegate this incident to the specified escalation policy. Cannot be specified if an assignee " +
                        "is given.")
                .options((OptionsFunction<String>) PagerDutyUtils::getEscalationPolicyIdOptions)
                .required(false))
        .output(outputSchema(INCIDENT_OBJECT))
        .perform(PagerDutyUpdateIncidentAction::perform);

    private PagerDutyUpdateIncidentAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(
            http -> http.put("/incidents/%s".formatted(inputParameters.getRequiredString(INCIDENT_ID))))
            .body(Body.of(Map.of(INCIDENT, getRequestBody(inputParameters))))
            .header(FROM, inputParameters.getRequiredString(FROM))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
