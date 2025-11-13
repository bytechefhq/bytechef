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

package com.bytechef.component.google.bigquery.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.CREATION_SESSION;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.DRY_RUN;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.MAX_RESULT;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.PROJECT_ID;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.QUERY;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.TABLE_FIELD_SCHEMA;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.TIMEOUT_MS;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.bigquery.util.GoogleBigQueryUtils;

/**
 * @author Nikolina Spehar
 */
public class GoogleBigQueryQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("query")
        .title("Query")
        .description(
            "Runs a BigQuery SQL query synchronously and returns query results if the query completes within a " +
                "specified timeout.")
        .properties(
            string(PROJECT_ID)
                .label("Project ID")
                .description("Project ID of the query request.")
                .options((OptionsFunction<String>) GoogleBigQueryUtils::getProjectIdOptions)
                .required(true),
            string(QUERY)
                .label("Query")
                .description(
                    "Required. A query string to execute, using Google Standard SQL or legacy SQL syntax. Example: " +
                        "\"SELECT COUNT(f1) FROM myProjectId.myDatasetId.myTableId\".")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            integer(MAX_RESULT)
                .label("Max Results")
                .description("The maximum number of rows of data to return per page of results.")
                .required(false),
            integer(TIMEOUT_MS)
                .label("Timeout")
                .description(
                    "Specifies the maximum amount of time, in milliseconds, that the client is willing to wait for " +
                        "the query to complete. By default, this limit is 10 seconds (10,000 milliseconds).")
                .required(false),
            bool(DRY_RUN)
                .label("Dry Run")
                .description(
                    "If set to true, BigQuery doesn't run the job. Instead, if the query is valid, BigQuery returns " +
                        "statistics about the job such as how many bytes would be processed.")
                .required(false),
            bool(CREATION_SESSION)
                .label("Create Session")
                .description(
                    "If true, creates a new session using a randomly generated sessionId. If false, runs query with " +
                        "an existing sessionId passed in ConnectionProperty.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("kind")
                            .description("The resource type."),
                        object("schema")
                            .description(
                                "The schema of the results. Present only when the query completes successfully.")
                            .properties(
                                array("fields")
                                    .description("Describes the fields in a table.")
                                    .items(TABLE_FIELD_SCHEMA)),
                        object("objectReference")
                            .description("Reference to the Job that was created to run the query.")
                            .properties(
                                string("projectId")
                                    .description("The ID of the project containing this job."),
                                string("jobId")
                                    .description("The ID of the job"),
                                string("location")
                                    .description("The geographic location of the job.")),
                        object("jobCreationReason")
                            .description("The reason why a Job was created.")
                            .properties(
                                string("code")
                                    .description("Specifies the high level reason why a Job was created.")),
                        string("queryId")
                            .description("Auto-generated ID for the query."),
                        string("location")
                            .description("The geographic location of the query."),
                        string("totalRows")
                            .description(
                                "The total number of rows in the complete query result set, which can be more than " +
                                    "the number of rows in this single page of results."),
                        string("pageToken")
                            .description("A token used for paging results."),
                        array("rows")
                            .description(
                                "An object with as many results as can be contained within the maximum permitted " +
                                    "reply size. ")
                            .items(object()),
                        string("totalBytesProcessed")
                            .description("The total number of bytes processed for this query."),
                        bool("jobComplete")
                            .description("Whether the query has completed or not."),
                        array("errors")
                            .description("The first errors or warnings encountered during the running of the job.")
                            .items(
                                object()
                                    .description("Error details.")
                                    .properties(
                                        string("reason")
                                            .description("A short error code that summarizes the error."),
                                        string("location")
                                            .description("Specifies where the error occurred, if present."),
                                        string("debugInfo")
                                            .description("Debugging information."),
                                        string("message")
                                            .description("A human-readable description of the error.")),
                                bool("cacheHit")
                                    .description("Whether the query result was fetched from the query cache."),
                                string("numDmlAffectedRows")
                                    .description("The number of rows affected by a DML statement."),
                                object("sessionInfo")
                                    .description("Information of the session if this job is part of one.")
                                    .properties(
                                        string("sessionId")
                                            .description("The id of the session.")),
                                object("dmlStats")
                                    .description(
                                        "Detailed statistics for DML statements INSERT, UPDATE, DELETE, MERGE or " +
                                            "TRUNCATE.")
                                    .properties(
                                        string("insertedRowCount")
                                            .description(
                                                "Number of inserted Rows. Populated by DML INSERT and MERGE " +
                                                    "statements."),
                                        string("deletedRowCount")
                                            .description(
                                                "Number of deleted Rows. populated by DML DELETE, MERGE and " +
                                                    "TRUNCATE statements."),
                                        string("updatedRowCount")
                                            .description(
                                                "Number of updated Rows. Populated by DML UPDATE and MERGE statements.")),
                                string("totalBytesBilled")
                                    .description(
                                        "If the project is configured to use on-demand pricing, then this field " +
                                            "contains the total bytes billed for the job."),
                                string("totalSlotMs")
                                    .description("Number of slot ms the user is actually billed for."),
                                string("creationTime")
                                    .description("Creation time of this query, in milliseconds since the epoch."),
                                string("startTime")
                                    .description("tart time of this query, in milliseconds since the epoch."),
                                string("endTime")
                                    .description("End time of this query, in milliseconds since the epoch.")))))
        .perform(GoogleBigQueryQueryAction::perform);

    private GoogleBigQueryQueryAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.post("https://bigquery.googleapis.com/bigquery/v2/projects/%s/queries"
                .formatted(inputParameters.getRequiredString(PROJECT_ID))))
            .configuration(responseType(Http.ResponseType.JSON))
            .body(
                Body.of(
                    QUERY, inputParameters.getRequiredString(QUERY),
                    MAX_RESULT, inputParameters.getInteger(MAX_RESULT),
                    TIMEOUT_MS, inputParameters.getInteger(TIMEOUT_MS),
                    DRY_RUN, inputParameters.getBoolean(DRY_RUN),
                    CREATION_SESSION, inputParameters.getBoolean(CREATION_SESSION)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
