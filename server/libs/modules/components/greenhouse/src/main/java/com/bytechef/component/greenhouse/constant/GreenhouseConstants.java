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
    public static final String STATUS = "status";

    public static final ModifiableObjectProperty APPLICATION_OUTPUT_PROPERTY = object()
        .properties(
            integer("id")
                .description("The ID of the application."),
            string("created_at")
                .description("ISO 8601 timestamp for when the application was created."),
            string("updated_at")
                .description("ISO 8601 timestamp for when the application was last updated."),
            integer("candidate_id")
                .description("The ID of the candidate this application belongs to."),
            integer("job_id")
                .description("The ID of the job this application is on. Null for jobless prospect applications."),
            integer("job_post_id")
                .description("The ID of the job post the candidate applied through."),
            integer("recruiter_id")
                .description("The ID of the user assigned as recruiter on the application's job."),
            integer("coordinator_id")
                .description("The ID of the user assigned as coordinator on the application's job."),
            integer("referrer_id")
                .description("The ID of the referrer credited with this application."),
            integer("source_id")
                .description("The ID of the source the application is attributed to."),
            integer("rejection_reason_id")
                .description("The ID of the rejection reason selected for the application."),
            integer("stage_id")
                .description("The ID of the interview stage the candidate is currently in."),
            integer("job_interview_stage_id")
                .description("The ID of the job interview stage definition of the current stage."),
            string("stage_name")
                .description("The display name of the current interview stage."),
            string(STATUS)
                .description("The lifecycle status of the application: in_process, rejected, hired or converted."),
            bool("needs_decision")
                .description("Whether the application is waiting on a hiring team decision."),
            bool("prospect")
                .description("Whether this is a prospect application."),
            string("rejected_at")
                .description("ISO 8601 timestamp for when the application was rejected."),
            string("last_activity_at")
                .description("ISO 8601 timestamp of recent activity for the application's candidate."),
            string("location_address")
                .description("The free-form location captured on the application."),
            array("answers")
                .description("The answers the candidate provided on the job post application form.")
                .items(
                    object()
                        .properties(
                            string("question"),
                            string("answer"))),
            array("prospective_job_ids")
                .description("For prospect applications, the IDs of the jobs the prospect is considered for.")
                .items(integer()),
            object("custom_fields")
                .description("Custom field values of the application."));

    public static final ModifiableObjectProperty CANDIDATE_OUTPUT_PROPERTY = object()
        .properties(
            integer("id")
                .description("The ID of the candidate."),
            string("created_at")
                .description("ISO 8601 timestamp for when the candidate was created."),
            string("updated_at")
                .description("ISO 8601 timestamp for when the candidate was last updated."),
            string("first_name")
                .description("The first name of the candidate."),
            string("last_name")
                .description("The last name of the candidate."),
            string("preferred_name")
                .description("The preferred name of the candidate."),
            string("company")
                .description("The current company of the candidate."),
            string("title")
                .description("The current job title of the candidate."),
            string("last_activity_at")
                .description("ISO 8601 timestamp of the most recent activity on the candidate's applications."),
            bool("private")
                .description("Whether the candidate is private."),
            bool("can_email")
                .description("Whether the candidate has consented to receive email."),
            string("time_zone")
                .description("The time zone of the candidate."),
            array("tags")
                .description("The tag names applied to the candidate.")
                .items(string()),
            array("linked_user_ids")
                .description("The IDs of Greenhouse users linked to the candidate.")
                .items(integer()),
            array("phone_numbers")
                .description("The phone numbers of the candidate.")
                .items(valueTypeObjectProperty()),
            array("addresses")
                .description("The postal addresses of the candidate.")
                .items(valueTypeObjectProperty()),
            array("email_addresses")
                .description("The email addresses of the candidate.")
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
            object("custom_fields")
                .description("Custom field values of the candidate."));

    public static final ModifiableObjectProperty JOB_OUTPUT_PROPERTY = object()
        .properties(
            integer("id")
                .description("The ID of the job."),
            string("created_at")
                .description("ISO 8601 timestamp for when the job was created."),
            string("updated_at")
                .description("ISO 8601 timestamp for when the job was last updated."),
            string("name")
                .description("The internal job title."),
            string("requisition_id")
                .description("The external requisition identifier of the job."),
            string("notes")
                .description("Internal HTML notes about the job."),
            bool("confidential")
                .description("Whether the job is confidential."),
            string(STATUS)
                .description("The lifecycle status of the job: draft, open or closed."),
            string("opened_at")
                .description("ISO 8601 timestamp for when the job first transitioned to open."),
            string("closed_at")
                .description("ISO 8601 timestamp for when the job most recently transitioned to closed."),
            bool("is_template")
                .description("Whether the job is a template."),
            integer("copied_from_id")
                .description("The ID of the job this job was copied from."),
            integer("department_id")
                .description("The ID of the department the job is assigned to."),
            string("external_department_id")
                .description("The external ID of the department the job is assigned to."),
            array("office_ids")
                .description("The IDs of the offices the job is assigned to.")
                .items(integer()),
            array("external_office_ids")
                .description("The external IDs of the offices the job is assigned to.")
                .items(string()),
            object("custom_fields")
                .description("Custom field values of the job."));

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
                    integer("application_id")
                        .description("The ID of the application the scorecard belongs to."),
                    integer("interview_kit_id")
                        .description("The ID of the interview kit the scorecard was filled out against."),
                    integer("interviewer_id")
                        .description("The ID of the user who participated in the interview."),
                    integer("submitter_id")
                        .description("The ID of the user who submitted the scorecard."),
                    string("interviewed_at")
                        .description("ISO 8601 timestamp for when the interview took place."),
                    string("submitted_at")
                        .description("ISO 8601 timestamp for when the scorecard was submitted."),
                    string(STATUS)
                        .description("The lifecycle status of the scorecard: draft or complete."),
                    string("candidate_rating")
                        .description("The interviewer's overall recommendation."),
                    string("notes")
                        .description("The key takeaways the interviewer recorded."),
                    string("public_notes")
                        .description("The note the interviewer left for other interviewers."),
                    string("private_notes")
                        .description("The private notes, visible only to users allowed to see private notes.")));

    private GreenhouseConstants() {
    }

    private static ModifiableObjectProperty valueTypeObjectProperty() {
        return object()
            .properties(
                string("value"),
                string("type"));
    }
}
