
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class StagesActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("getStages")
        .display(
            display("Get all stages")
                .description("Returns data about all stages."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/stages"

            ))
        .properties(integer("pipeline_id").label("Pipeline_id")
            .description(
                "The ID of the pipeline to fetch stages for. If omitted, stages for all pipelines will be fetched.")
            .required(false)
            .metadata(
                Map.of(
                    "type", "QUERY")))
        .output(object(null).properties(bool("success").label("Success")
            .description("If the request was successful or not")
            .required(false),
            array("data").items(object(null).properties(string("update_time").label("Update_time")
                .description("The stage update time. Format: YYYY-MM-DD HH:MM:SS.")
                .required(false),
                bool("pipeline_deal_probability").label("Pipeline_deal_probability")
                    .description("The pipeline deal probability. When `true`, overrides the stage probability.")
                    .required(false),
                integer("order_nr").label("Order_nr")
                    .description("Defines the order of the stage")
                    .required(false),
                string("name").label("Name")
                    .description("The name of the stage")
                    .required(false),
                integer("pipeline_id").label("Pipeline_id")
                    .description("The ID of the pipeline to add the stage to")
                    .required(false),
                integer("deal_probability").label("Deal_probability")
                    .description(
                        "The success probability percentage of the deal. Used/shown when the deal weighted values are used.")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the stage is active or deleted")
                    .required(false),
                bool("rotten_flag").label("Rotten_flag")
                    .description("Whether deals in this stage can become rotten")
                    .options(option("True", true), option("False", false))
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the stage")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The stage creation time. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                integer("rotten_days").label("Rotten_days")
                    .description(
                        "The number of days the deals not updated in this stage would become rotten. Applies only if the `rotten_flag` is set.")
                    .required(false),
                string("pipeline_name").label("Pipeline_name")
                    .description("The name of the pipeline")
                    .required(false))
                .description("The array of stages"))
                .placeholder("Add")
                .label("Data")
                .description("The array of stages")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", "JSON")))
        .exampleOutput(
            "{\"success\":true,\"data\":[{\"id\":1,\"order_nr\":1,\"name\":\"Stage Name\",\"active_flag\":true,\"deal_probability\":100,\"pipeline_id\":1,\"rotten_flag\":false,\"rotten_days\":1,\"add_time\":\"2017-08-03 12:51:18\",\"update_time\":\"2020-03-23 13:15:25\",\"pipeline_name\":\"Pipeline\",\"pipeline_deal_probability\":false}]}"),
        action("getStage")
            .display(
                display("Get one stage")
                    .description("Returns data about a specific stage."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/stages/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the stage")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")),
                number("everyone").label("Everyone")
                    .description("If `everyone=1` is provided, deals summary will return deals owned by every user")
                    .options(option("0", 0), option("1", 1))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")))
            .output(object(null).properties(bool("success").label("Success")
                .description("If the request was successful or not")
                .required(false),
                string("update_time").label("Update_time")
                    .description("The stage update time. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                object("deals_summary")
                    .properties(
                        object("per_stages")
                            .properties(object("STAGE_ID").properties(object("CURRENCY_ID").properties(integer("count")
                                .label("Count")
                                .description("Deals count per currency")
                                .required(false),
                                integer("value").label("Value")
                                    .description("Deals value per currency")
                                    .required(false),
                                string("value_formatted").label("Value_formatted")
                                    .description("Deals value formatted per currency")
                                    .required(false),
                                integer("weighted_value").label("Weighted_value")
                                    .description("Deals weighted value per currency")
                                    .required(false),
                                string("weighted_value_formatted").label("Weighted_value_formatted")
                                    .description("Deals weighted value formatted per currency")
                                    .required(false))
                                .label("CURRENCY_ID")
                                .description(
                                    "The currency summary. This parameter is dynamic and changes according to `currency_id` value.")
                                .required(false))
                                .label("STAGE_ID")
                                .description(
                                    "The currency summaries per stage. This parameter is dynamic and changes according to `stage_id` value.")
                                .required(false))
                            .label("Per_stages")
                            .description("The stage objects containing deals currency information")
                            .required(false),
                        object("per_currency").properties(integer("CURRENCY_ID").label("CURRENCY_ID")
                            .description(
                                "Deals count per currency. This parameter is dynamic and changes according to `currency_id` value.")
                            .required(false))
                            .label("Per_currency")
                            .description("The currency count summary")
                            .required(false),
                        integer("total_count").label("Total_count")
                            .description("Deals count")
                            .required(false),
                        object("per_currency_full").properties(object("CURRENCY_ID").properties(integer("count")
                            .label("Count")
                            .description("Deals count per currency")
                            .required(false),
                            integer("value").label("Value")
                                .description("Deals value per currency")
                                .required(false))
                            .label("CURRENCY_ID")
                            .description(
                                "The currency summary. This parameter is dynamic and changes according to `currency_id` value.")
                            .required(false))
                            .label("Per_currency_full")
                            .description("Full currency summaries")
                            .required(false))
                    .label("Deals_summary")
                    .description("Deals summary")
                    .required(false),
                integer("order_nr").label("Order_nr")
                    .description("Defines the order of the stage")
                    .required(false),
                string("name").label("Name")
                    .description("The name of the stage")
                    .required(false),
                integer("pipeline_id").label("Pipeline_id")
                    .description("The ID of the pipeline to add the stage to")
                    .required(false),
                integer("deal_probability").label("Deal_probability")
                    .description(
                        "The success probability percentage of the deal. Used/shown when the deal weighted values are used.")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the stage is active or deleted")
                    .required(false),
                bool("rotten_flag").label("Rotten_flag")
                    .description("Whether deals in this stage can become rotten")
                    .options(option("True", true), option("False", false))
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the stage")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The stage creation time. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                integer("rotten_days").label("Rotten_days")
                    .description(
                        "The number of days the deals not updated in this stage would become rotten. Applies only if the `rotten_flag` is set.")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":1,\"order_nr\":1,\"name\":\"Stage Name\",\"active_flag\":true,\"deal_probability\":100,\"pipeline_id\":1,\"rotten_flag\":false,\"rotten_days\":1,\"add_time\":\"2017-08-03 12:51:18\",\"update_time\":\"2020-03-23 13:15:25\",\"deals_summary\":{\"per_stages\":{\"1\":{\"EUR\":{\"count\":1,\"value\":10,\"value_formatted\":\"10 €\",\"weighted_value\":10,\"weighted_value_formatted\":\"10€\"}}},\"per_currency\":{\"EUR\":1},\"total_count\":1,\"per_currency_full\":{\"EUR\":{\"count\":1,\"value\":10}}}}}"));
}
