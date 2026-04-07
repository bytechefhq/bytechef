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

package com.bytechef.component.apify.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.apify.util.ApifyUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ApifyGetLastRunAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getLastRun")
        .title("Get Last Run")
        .description("Get Apify Actor last run.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/acts/{actorId}/runs/last"

            ))
        .properties(string("actorId").label("Actor ID")
            .description("ID of the actor that will be fetched.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) ApifyUtils::getActorIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("data").properties(string("id").description("Unique identifier of the run")
                .required(false),
                string("actId").description("Identifier of the actor")
                    .required(false),
                string("userId").description("Identifier of the user who started the run")
                    .required(false),
                string("actorTaskId").description("Identifier of the actor task")
                    .required(false),
                dateTime("startedAt").description("Timestamp when the run started")
                    .required(false),
                dateTime("finishedAt").description("Timestamp when the run finished")
                    .required(false),
                string("status").description("Current status of the run")
                    .required(false),
                string("statusMessage").description("Human-readable status message")
                    .required(false),
                bool("isStatusMessageTerminal").description("Indicates if the status message is terminal")
                    .required(false),
                object("meta").properties(string("origin").description("Origin of the run")
                    .required(false),
                    string("clientIp").description("IP address of the client")
                        .required(false),
                    string("userAgent").description("User agent string of the client")
                        .required(false),
                    string("scheduleId").description("Identifier of the schedule")
                        .required(false),
                    dateTime("scheduledAt").description("Scheduled execution time")
                        .required(false))
                    .description("Metadata about the run origin and environment")
                    .required(false),
                object("pricingInfo")
                    .properties(number("apifyMarginPercentage").description("Margin percentage applied")
                        .required(false),
                        dateTime("createdAt").description("Timestamp when pricing info was created")
                            .required(false),
                        dateTime("startedAt").description("Timestamp when pricing started")
                            .required(false),
                        dateTime("notifiedAboutFutureChangeAt")
                            .description("Timestamp of future pricing change notification")
                            .required(false),
                        dateTime("notifiedAboutChangeAt").description("Timestamp of pricing change notification")
                            .required(false),
                        string("reasonForChange").description("Reason for pricing change")
                            .required(false),
                        string("pricingModel").description("Pricing model used")
                            .required(false),
                        object("pricingPerEvent")
                            .properties(object("actorChargeEvents").description("Mapping of chargeable events")
                                .required(false))
                            .description("Pricing details per event")
                            .required(false),
                        number("minimalMaxTotalChargeUsd").description("Minimum maximum total charge in USD")
                            .required(false))
                    .description("Pricing details for the run")
                    .required(false),
                object("stats").properties(integer("inputBodyLen").description("Length of the input body")
                    .required(false),
                    integer("migrationCount").description("Number of migrations")
                        .required(false),
                    integer("rebootCount").description("Number of reboots")
                        .required(false),
                    integer("restartCount").description("Number of restarts")
                        .required(false),
                    integer("resurrectCount").description("Number of resurrects")
                        .required(false),
                    number("memAvgBytes").description("Average memory usage in bytes")
                        .required(false),
                    number("memMaxBytes").description("Maximum memory usage in bytes")
                        .required(false),
                    number("memCurrentBytes").description("Current memory usage in bytes")
                        .required(false),
                    number("cpuAvgUsage").description("Average CPU usage")
                        .required(false),
                    number("cpuMaxUsage").description("Maximum CPU usage")
                        .required(false),
                    number("cpuCurrentUsage").description("Current CPU usage")
                        .required(false),
                    number("netRxBytes").description("Network received bytes")
                        .required(false),
                    number("netTxBytes").description("Network transmitted bytes")
                        .required(false),
                    integer("durationMillis").description("Duration of the run in milliseconds")
                        .required(false),
                    number("runTimeSecs").description("Runtime in seconds")
                        .required(false),
                    integer("metamorph").description("Number of metamorph operations")
                        .required(false),
                    number("computeUnits").description("Compute units consumed")
                        .required(false))
                    .description("Runtime statistics of the run")
                    .required(false),
                object("chargedEventCounts").additionalProperties(integer())
                    .description("Counts of charged events by event type")
                    .required(false),
                object("options").properties(string("build").description("Build tag used")
                    .required(false),
                    integer("timeoutSecs").description("Timeout in seconds")
                        .required(false),
                    integer("memoryMbytes").description("Allocated memory in megabytes")
                        .required(false),
                    integer("diskMbytes").description("Allocated disk space in megabytes")
                        .required(false),
                    integer("maxItems").description("Maximum number of items to process")
                        .required(false),
                    number("maxTotalChargeUsd").description("Maximum total charge in USD")
                        .required(false))
                    .description("Run configuration options")
                    .required(false),
                string("buildId").description("Identifier of the build")
                    .required(false),
                integer("exitCode").description("Exit code of the run")
                    .required(false),
                string("generalAccess").description("Access level of the run")
                    .required(false),
                string("defaultKeyValueStoreId").description("Default key-value store ID")
                    .required(false),
                string("defaultDatasetId").description("Default dataset ID")
                    .required(false),
                string("defaultRequestQueueId").description("Default request queue ID")
                    .required(false),
                object("storageIds")
                    .properties(object("datasets").properties(string("default").description("Default dataset ID")
                        .required(false))
                        .required(false),
                        object("keyValueStores").properties(string("default").description("Default key-value store ID")
                            .required(false))
                            .required(false),
                        object("requestQueues").properties(string("default").description("Default request queue ID")
                            .required(false))
                            .required(false))
                    .description("Identifiers of associated storage resources")
                    .required(false),
                string("buildNumber").description("Build version number")
                    .required(false),
                string("containerUrl").description("URL of the container")
                    .required(false),
                bool("isContainerServerReady").description("Indicates if the container server is ready")
                    .required(false),
                string("gitBranchName").description("Git branch name used for the build")
                    .required(false),
                object("usage").additionalProperties(number())
                    .description("Resource usage metrics")
                    .required(false),
                number("usageTotalUsd").description("Total usage cost in USD")
                    .required(false),
                object("usageUsd").additionalProperties(number())
                    .description("Cost breakdown per resource type in USD")
                    .required(false),
                array("metamorphs")
                    .items(object().properties(dateTime("createdAt").description("Timestamp when metamorph was created")
                        .required(false),
                        string("actorId").description("Identifier of the actor")
                            .required(false),
                        string("buildId").description("Identifier of the build")
                            .required(false),
                        string("inputKey").description("Input key used for metamorph")
                            .required(false))
                        .description("List of metamorph operations"))
                    .description("List of metamorph operations")
                    .required(false))
                .description("Main run object containing execution details")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/apify_v1#get-last-run");

    private ApifyGetLastRunAction() {
    }
}
