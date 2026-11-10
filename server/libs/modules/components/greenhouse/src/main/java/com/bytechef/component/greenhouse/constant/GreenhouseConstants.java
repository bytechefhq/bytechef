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

package com.bytechef.component.greenhouse.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Ivica Cardic
 */
public class GreenhouseConstants {

    public static final String APPLICATION_ID = "applicationId";
    public static final String CANDIDATE_ID = "candidateId";
    public static final String JOB_ID = "jobId";

    public static final ModifiableObjectProperty APPLICATION_OUTPUT_PROPERTY = object()
        .properties(
            integer("id")
                .description("The ID of the application."),
            integer("candidate_id")
                .description("The ID of the candidate who submitted the application."),
            bool("prospect")
                .description("Whether the application is a prospect application rather than a candidate one."),
            string("applied_at")
                .description("ISO 8601 timestamp for when the application was submitted."),
            string("rejected_at")
                .description("ISO 8601 timestamp for when the application was rejected, if it was."),
            string("last_activity_at")
                .description("ISO 8601 timestamp of the last activity on the application."),
            string("status")
                .description("The status of the application: active, rejected, hired or converted."),
            object("source")
                .description("The source of the application.")
                .properties(
                    integer("id"),
                    string("public_name")),
            userObjectProperty("credited_to", "The user credited with sourcing the application."),
            idNameObjectProperty("current_stage", "The current interview stage of the application."),
            array("jobs")
                .description("The jobs the application is for.")
                .items(idNameObjectProperty()),
            integer("job_post_id")
                .description("The ID of the job post the application was submitted through."),
            object("rejection_reason")
                .description("The reason the application was rejected, if it was."),
            array("answers")
                .description("The answers to the job post application questions.")
                .items(
                    object()
                        .properties(
                            string("question"),
                            string("answer"))),
            array("attachments")
                .description("Files attached to the application. Attachment URLs expire after 7 days.")
                .items(
                    object()
                        .properties(
                            string("filename"),
                            string("url"),
                            string("type"),
                            string("created_at"))),
            object("custom_fields")
                .description("Custom field values of the application, keyed by field name."));

    public static final ModifiableObjectProperty CANDIDATE_OUTPUT_PROPERTY = object()
        .properties(
            integer("id")
                .description("The ID of the candidate."),
            string("first_name")
                .description("The first name of the candidate."),
            string("last_name")
                .description("The last name of the candidate."),
            string("company")
                .description("The current company of the candidate."),
            string("title")
                .description("The current job title of the candidate."),
            string("created_at")
                .description("ISO 8601 timestamp for when the candidate was created."),
            string("updated_at")
                .description("ISO 8601 timestamp for when the candidate was last updated."),
            string("last_activity")
                .description("ISO 8601 timestamp of the last activity on the candidate."),
            bool("is_private")
                .description("Whether the candidate is private."),
            bool("can_email")
                .description("Whether the candidate can be emailed."),
            array("application_ids")
                .description("The IDs of the applications of the candidate.")
                .items(integer()),
            array("phone_numbers")
                .description("The phone numbers of the candidate.")
                .items(valueTypeObjectProperty()),
            array("email_addresses")
                .description("The email addresses of the candidate.")
                .items(valueTypeObjectProperty()),
            array("addresses")
                .description("The addresses of the candidate.")
                .items(valueTypeObjectProperty()),
            array("website_addresses")
                .description("The website addresses of the candidate.")
                .items(valueTypeObjectProperty()),
            array("social_media_addresses")
                .description("The social media addresses of the candidate.")
                .items(
                    object()
                        .properties(
                            string("value"))),
            userObjectProperty("recruiter", "The recruiter assigned to the candidate."),
            userObjectProperty("coordinator", "The coordinator assigned to the candidate."),
            array("tags")
                .description("The tags of the candidate.")
                .items(string()),
            array("applications")
                .description("The applications of the candidate.")
                .items(APPLICATION_OUTPUT_PROPERTY),
            object("custom_fields")
                .description("Custom field values of the candidate, keyed by field name."));

    public static final ModifiableObjectProperty JOB_OUTPUT_PROPERTY = object()
        .properties(
            integer("id")
                .description("The ID of the job."),
            string("name")
                .description("The name of the job."),
            string("requisition_id")
                .description("The requisition ID of the job."),
            string("notes")
                .description("Notes on the job."),
            bool("confidential")
                .description("Whether the job is confidential."),
            string("status")
                .description("The status of the job: open, closed or draft."),
            string("created_at")
                .description("ISO 8601 timestamp for when the job was created."),
            string("opened_at")
                .description("ISO 8601 timestamp for when the job was opened."),
            string("closed_at")
                .description("ISO 8601 timestamp for when the job was closed."),
            string("updated_at")
                .description("ISO 8601 timestamp for when the job was last updated."),
            bool("is_template")
                .description("Whether the job is a template."),
            integer("copied_from_id")
                .description("The ID of the job this job was copied from."),
            array("departments")
                .description("The departments of the job.")
                .items(idNameObjectProperty()),
            array("offices")
                .description("The offices of the job.")
                .items(idNameObjectProperty()),
            object("hiring_team")
                .description("The hiring team of the job: hiring managers, recruiters, coordinators and sourcers."),
            array("openings")
                .description("The openings of the job.")
                .items(
                    object()
                        .properties(
                            integer("id"),
                            string("opening_id"),
                            string("status"),
                            string("opened_at"),
                            string("closed_at"),
                            integer("application_id"))),
            object("custom_fields")
                .description("Custom field values of the job, keyed by field name."));

    public static final ModifiableArrayProperty SCORECARDS_OUTPUT_PROPERTY = array()
        .items(
            object()
                .properties(
                    integer("id")
                        .description("The ID of the scorecard."),
                    string("created_at")
                        .description("ISO 8601 timestamp for when the scorecard was created."),
                    string("updated_at")
                        .description("ISO 8601 timestamp for when the scorecard was last updated."),
                    string("interview")
                        .description("The name of the interview."),
                    idNameObjectProperty("interview_step", "The interview step the scorecard was submitted for."),
                    integer("candidate_id")
                        .description("The ID of the candidate."),
                    integer("application_id")
                        .description("The ID of the application."),
                    string("interviewed_at")
                        .description("ISO 8601 timestamp for when the interview took place."),
                    userObjectProperty("submitted_by", "The user who submitted the scorecard."),
                    userObjectProperty("interviewer", "The user who conducted the interview."),
                    string("submitted_at")
                        .description("ISO 8601 timestamp for when the scorecard was submitted."),
                    string("overall_recommendation")
                        .description(
                            "The overall recommendation: definitely_not, no, mixed, yes, strong_yes or no_decision."),
                    array("attributes")
                        .description("The rated attributes of the scorecard.")
                        .items(
                            object()
                                .properties(
                                    string("name"),
                                    string("type"),
                                    string("note"),
                                    string("rating"))),
                    object("ratings")
                        .description("The attribute names grouped by rating."),
                    array("questions")
                        .description("The scorecard questions and their answers.")
                        .items(
                            object()
                                .properties(
                                    integer("id"),
                                    string("question"),
                                    string("answer")))));

    private GreenhouseConstants() {
    }

    private static ModifiableObjectProperty idNameObjectProperty() {
        return object()
            .properties(
                integer("id"),
                string("name"));
    }

    private static ModifiableObjectProperty idNameObjectProperty(String name, String description) {
        return object(name)
            .description(description)
            .properties(
                integer("id"),
                string("name"));
    }

    private static ModifiableObjectProperty userObjectProperty(String name, String description) {
        return object(name)
            .description(description)
            .properties(
                integer("id")
                    .description("The ID of the user."),
                string("first_name")
                    .description("The first name of the user."),
                string("last_name")
                    .description("The last name of the user."),
                string("name")
                    .description("The full name of the user."),
                string("employee_id")
                    .description("The employee ID of the user."));
    }

    private static ModifiableObjectProperty valueTypeObjectProperty() {
        return object()
            .properties(
                string("value"),
                string("type"));
    }
}
