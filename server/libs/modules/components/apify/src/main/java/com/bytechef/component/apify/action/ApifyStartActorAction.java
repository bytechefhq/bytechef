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

import static com.bytechef.component.apify.constant.ApifyConstants.ACTOR_ID;
import static com.bytechef.component.apify.constant.ApifyConstants.BODY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.apify.util.ApifyUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ApifyStartActorAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("startActor")
        .title("Start Actor")
        .description("Starts an Apify Actor")
        .properties(
            string(ACTOR_ID)
                .label("Actor ID")
                .description("ID of the actor that will be run.")
                .required(true)
                .options((OptionsFunction<String>) ApifyUtils::getActorIdOptions),
            string(BODY)
                .label("Body")
                .description(
                    "The JSON input to pass to the Actor [you can get the JSON from a run in your Apify account].")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .description("Main run object containing execution details")
                            .properties(
                                string("id")
                                    .description("Unique identifier of the run"),
                                string("actId")
                                    .description("Identifier of the actor"),
                                string("userId")
                                    .description("Identifier of the user who started the run"),
                                string("actorTaskId")
                                    .description("Identifier of the actor task"),
                                string("startedAt")
                                    .description("Timestamp when the run started"),
                                string("finishedAt")
                                    .description("Timestamp when the run finished"),
                                string("status")
                                    .description("Current status of the run"),
                                string("statusMessage")
                                    .description("Human-readable status message"),
                                bool("isStatusMessageTerminal")
                                    .description("Indicates if the status message is terminal"),
                                object("meta")
                                    .description("Metadata about the run origin and environment")
                                    .properties(
                                        string("origin")
                                            .description("Origin of the run"),
                                        string("clientIp")
                                            .description("IP address of the client"),
                                        string("userAgent")
                                            .description("User agent string of the client"),
                                        string("scheduleId")
                                            .description("Identifier of the schedule"),
                                        string("scheduledAt")
                                            .description("Scheduled execution time")),
                                object("pricingInfo")
                                    .description("Pricing details for the run")
                                    .properties(
                                        number("apifyMarginPercentage")
                                            .description("Margin percentage applied"),
                                        string("createdAt")
                                            .description("Timestamp when pricing info was created"),
                                        string("startedAt")
                                            .description("Timestamp when pricing started"),
                                        string("notifiedAboutFutureChangeAt")
                                            .description("Timestamp of future pricing change notification"),
                                        string("notifiedAboutChangeAt")
                                            .description("Timestamp of pricing change notification"),
                                        string("reasonForChange")
                                            .description("Reason for pricing change"),
                                        string("pricingModel")
                                            .description("Pricing model used"),
                                        object("pricingPerEvent")
                                            .description("Pricing details per event")
                                            .properties(
                                                object("actorChargeEvents")
                                                    .description("Mapping of chargeable events")),
                                        number("minimalMaxTotalChargeUsd")
                                            .description("Minimum maximum total charge in USD")),
                                object("stats")
                                    .description("Runtime statistics of the run")
                                    .properties(
                                        integer("inputBodyLen")
                                            .description("Length of the input body"),
                                        integer("migrationCount")
                                            .description("Number of migrations"),
                                        integer("rebootCount")
                                            .description("Number of reboots"),
                                        integer("restartCount")
                                            .description("Number of restarts"),
                                        integer("resurrectCount")
                                            .description("Number of resurrects"),
                                        number("memAvgBytes")
                                            .description("Average memory usage in bytes"),
                                        number("memMaxBytes")
                                            .description("Maximum memory usage in bytes"),
                                        number("memCurrentBytes")
                                            .description("Current memory usage in bytes"),
                                        number("cpuAvgUsage")
                                            .description("Average CPU usage"),
                                        number("cpuMaxUsage")
                                            .description("Maximum CPU usage"),
                                        number("cpuCurrentUsage")
                                            .description("Current CPU usage"),
                                        number("netRxBytes")
                                            .description("Network received bytes"),
                                        number("netTxBytes")
                                            .description("Network transmitted bytes"),
                                        integer("durationMillis")
                                            .description("Duration of the run in milliseconds"),
                                        number("runTimeSecs")
                                            .description("Runtime in seconds"),
                                        integer("metamorph")
                                            .description("Number of metamorph operations"),
                                        number("computeUnits")
                                            .description("Compute units consumed")),
                                object("chargedEventCounts")
                                    .description("Counts of charged events by event type")
                                    .properties(
                                        integer("actor-start"),
                                        integer("page-crawled"),
                                        integer("data-extracted")),
                                object("options")
                                    .description("Run configuration options")
                                    .properties(
                                        string("build")
                                            .description("Build tag used"),
                                        integer("timeoutSecs")
                                            .description("Timeout in seconds"),
                                        integer("memoryMbytes")
                                            .description("Allocated memory in megabytes"),
                                        integer("diskMbytes")
                                            .description("Allocated disk space in megabytes"),
                                        integer("maxItems")
                                            .description("Maximum number of items to process"),
                                        number("maxTotalChargeUsd")
                                            .description("Maximum total charge in USD")),
                                string("buildId")
                                    .description("Identifier of the build"),
                                integer("exitCode")
                                    .description("Exit code of the run"),
                                string("generalAccess")
                                    .description("Access level of the run"),
                                string("defaultKeyValueStoreId")
                                    .description("Default key-value store ID"),
                                string("defaultDatasetId")
                                    .description("Default dataset ID"),
                                string("defaultRequestQueueId")
                                    .description("Default request queue ID"),
                                object("storageIds")
                                    .description("Identifiers of associated storage resources")
                                    .properties(
                                        object("datasets")
                                            .properties(
                                                string("default")
                                                    .description("Default dataset ID")),
                                        object("keyValueStores")
                                            .properties(
                                                string("default")
                                                    .description("Default key-value store ID")),
                                        object("requestQueues")
                                            .properties(
                                                string("default")
                                                    .description("Default request queue ID"))),
                                string("buildNumber")
                                    .description("Build version number"),
                                string("containerUrl")
                                    .description("URL of the container"),
                                bool("isContainerServerReady")
                                    .description("Indicates if the container server is ready"),
                                string("gitBranchName")
                                    .description("Git branch name used for the build"),
                                object("usage")
                                    .description("Resource usage metrics")
                                    .properties(
                                        integer("ACTOR_COMPUTE_UNITS"),
                                        integer("DATASET_READS"),
                                        integer("DATASET_WRITES"),
                                        integer("KEY_VALUE_STORE_READS"),
                                        integer("KEY_VALUE_STORE_WRITES"),
                                        integer("KEY_VALUE_STORE_LISTS"),
                                        integer("REQUEST_QUEUE_READS"),
                                        integer("REQUEST_QUEUE_WRITES"),
                                        integer("DATA_TRANSFER_INTERNAL_GBYTES"),
                                        integer("DATA_TRANSFER_EXTERNAL_GBYTES"),
                                        integer("PROXY_RESIDENTIAL_TRANSFER_GBYTES"),
                                        integer("PROXY_SERPS")),
                                number("usageTotalUsd")
                                    .description("Total usage cost in USD"),
                                object("usageUsd")
                                    .description("Cost breakdown per resource type in USD")
                                    .properties(
                                        integer("ACTOR_COMPUTE_UNITS"),
                                        integer("DATASET_READS"),
                                        integer("DATASET_WRITES"),
                                        integer("KEY_VALUE_STORE_READS"),
                                        integer("KEY_VALUE_STORE_WRITES"),
                                        integer("KEY_VALUE_STORE_LISTS"),
                                        integer("REQUEST_QUEUE_READS"),
                                        integer("REQUEST_QUEUE_WRITES"),
                                        integer("DATA_TRANSFER_INTERNAL_GBYTES"),
                                        integer("DATA_TRANSFER_EXTERNAL_GBYTES"),
                                        integer("PROXY_RESIDENTIAL_TRANSFER_GBYTES"),
                                        integer("PROXY_SERPS")),
                                array("metamorphs")
                                    .description("List of metamorph operations")
                                    .items(
                                        object()
                                            .properties(
                                                string("createdAt")
                                                    .description("Timestamp when metamorph was created"),
                                                string("actorId")
                                                    .description("Identifier of the actor"),
                                                string("buildId")
                                                    .description("Identifier of the build"),
                                                string("inputKey")
                                                    .description("Input key used for metamorph")))))))
        .help("", "https://docs.bytechef.io/reference/components/apify_v1#start-actor")
        .perform(ApifyStartActorAction::perform);

    private ApifyStartActorAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> jsonBody = context.json(
            json -> json.read(inputParameters.getRequiredString(BODY), new TypeReference<>() {}));

        return context.http(
            http -> http.post("/acts/%s/runs".formatted(inputParameters.getRequiredString(ACTOR_ID))))
            .body(Body.of(jsonBody))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
