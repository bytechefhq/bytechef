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

package com.bytechef.component.pagerduty.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class PagerDutyConstants {

    public static final String API_KEY = "api_key";
    public static final String ASSIGNEE = "assignee";
    public static final String ASSIGNMENTS = "assignments";
    public static final String BODY = "body";
    public static final String CONTENT = "content";
    public static final String DETAILS = "details";
    public static final String ESCALATION_POLICY = "escalation_policy";
    public static final String FROM = "From";
    public static final String ID = "id";
    public static final String INCIDENT = "incident";
    public static final String INCIDENT_ID = "incidentId";
    public static final String INCIDENT_KEY = "incident_key";
    public static final String INCIDENT_TYPE = "incident_type";
    public static final String NAME = "name";
    public static final String PRIORITY = "priority";
    public static final String RESOLUTION = "resolution";
    public static final String SERVICE = "service";
    public static final String STATUS = "status";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String URGENCY = "urgency";

    public static final ModifiableObjectProperty INCIDENT_OBJECT = object()
        .properties(
            object("incident")
                .properties(
                    integer("incident_number")
                        .description("Incident number."),
                    string("title")
                        .description("Title of the incident."),
                    string("description")
                        .description("Description of the incident."),
                    string("created_at")
                        .description("Date when the incident was created."),
                    string("updated_at")
                        .description("Date when the incident was updated."),
                    string("status")
                        .description("Status of the incident."),
                    string("incident_key")
                        .description("Incident key."),
                    object("service")
                        .description("The user who created an incident note.")
                        .properties(
                            string("id")
                                .description("ID of the object."),
                            string("type")
                                .description("A string that determines the schema of the object."),
                            string("summary")
                                .description("A short description about the service."),
                            string("self")
                                .description("The API show URL at which the object is accessible."),
                            string("html_url")
                                .description(
                                    "A URL at which the entity is uniquely displayed in the Web app.")),
                    array("assignments")
                        .description("Assignments of the incident.")
                        .items(
                            object()
                                .properties(
                                    string("at")
                                        .description(
                                            "Date when the incident was assigned to the assignee."),
                                    object("assignee")
                                        .description("Assignee of the incident.")
                                        .properties(
                                            string("id")
                                                .description("ID of the object."),
                                            string("type")
                                                .description(
                                                    "A string that determines the schema of the object."),
                                            string("summary")
                                                .description("A short description about the assignee."),
                                            string("self")
                                                .description(
                                                    "The API show URL at which the object is accessible."),
                                            string("html_url")
                                                .description(
                                                    "A URL at which the entity is uniquely displayed in the Web app.")))),
                    string("assigned_via")
                        .description("How was incident assigned."),
                    string("last_status_change_at")
                        .description("Date when the status of the incident was last changed."),
                    string("resolved_at")
                        .description("Date when the status of the incident was resolved."),
                    object("first_trigger_log_entry")
                        .description("First trigger log entry of the incident.")
                        .properties(
                            string("id")
                                .description("ID of the object."),
                            string("type")
                                .description("A string that determines the schema of the object."),
                            string("summary")
                                .description("A short description about the log entry."),
                            string("self")
                                .description("The API show URL at which the object is accessible."),
                            string("html_url")
                                .description(
                                    "A URL at which the entity is uniquely displayed in the Web app.")),
                    object("alert_counts")
                        .description("Alert counts of the incident.")
                        .properties(
                            integer("all")
                                .description("Number of all alerts."),
                            integer("triggered")
                                .description("Number of triggered alerts."),
                            integer("resolved")
                                .description("Number of resolved triggers.")),
                    bool("is_mergeable")
                        .description("Whether the incident can be merged."),
                    object("incident_type")
                        .description("Incident type.")
                        .properties(
                            string("name")
                                .description("Name of the incident type.")),
                    object("escalation_policy")
                        .description("Escalation Policy.")
                        .properties(
                            string("id")
                                .description("ID of the object."),
                            string("type")
                                .description("A string that determines the schema of the object."),
                            string("summary")
                                .description("A short description about the escalation policy."),
                            string("self")
                                .description("The API show URL at which the object is accessible."),
                            string("html_url")
                                .description(
                                    "A URL at which the entity is uniquely displayed in the Web app.")),
                    array("teams")
                        .description("Teams that are associated with the incident."),
                    array("impacted_services")
                        .description("Impacted services.")
                        .items(
                            object("service")
                                .description("Impacted service.")
                                .properties(
                                    string("id")
                                        .description("ID of the object."),
                                    string("type")
                                        .description("A string that determines the schema of the object."),
                                    string("summary")
                                        .description("A short description about the impacted services."),
                                    string("self")
                                        .description("The API show URL at which the object is accessible."),
                                    string("html_url")
                                        .description(
                                            "A URL at which the entity is uniquely displayed in the Web app."))),
                    array("pending_actions")
                        .description("Pending actions."),
                    array("acknowledgements")
                        .description("Acknowledgements for the incident."),
                    string("basic_alert_grouping")
                        .description("Basic alert grouping."),
                    string("alert_grouping")
                        .description("Alert grouping."),
                    object("last_status_changed_by")
                        .description("User that changed the last status.")
                        .properties(
                            string("id")
                                .description("ID of the object."),
                            string("type")
                                .description("A string that determines the schema of the object."),
                            string("summary")
                                .description("A short description about the user."),
                            string("self")
                                .description("The API show URL at which the object is accessible."),
                            string("html_url")
                                .description(
                                    "A URL at which the entity is uniquely displayed in the Web app.")),
                    object("priority")
                        .description("Priority of the incident.")
                        .properties(
                            string("id")
                                .description("ID of the object."),
                            string("type")
                                .description("A string that determines the schema of the object."),
                            string("summary")
                                .description("A short description about the user."),
                            string("self")
                                .description("The API show URL at which the object is accessible."),
                            string("html_url")
                                .description(
                                    "A URL at which the entity is uniquely displayed in the Web app."),
                            string("account_id")
                                .description("ID of the account that set the priority."),
                            string("color")
                                .description("Color of the priority."),
                            string("created_at")
                                .description("Date when the priority was set."),
                            string("description")
                                .description("Description of the priority."),
                            string("name")
                                .description("Name of the priority."),
                            integer("order")
                                .description("Order of the priority."),
                            integer("schema_version")
                                .description("Schema version of the priority."),
                            string("updated_at")
                                .description("Date when the priority was updated.")),
                    array("incidents_responders")
                        .description("Responders of the incident."),
                    array("responder_requests")
                        .description("Requests of the incident responders."),
                    array("subscriber_requests")
                        .description("Requests of the incident subscribers"),
                    string("urgency")
                        .description("Urgency of the incident."),
                    string("id")
                        .description("ID of the object."),
                    string("type")
                        .description("A string that determines the schema of the object."),
                    string("summary")
                        .description("A short description about the incident."),
                    string("self")
                        .description("The API show URL at which the object is accessible."),
                    string("html_url")
                        .description("A URL at which the entity is uniquely displayed in the Web app.")));

    private PagerDutyConstants() {
    }
}
