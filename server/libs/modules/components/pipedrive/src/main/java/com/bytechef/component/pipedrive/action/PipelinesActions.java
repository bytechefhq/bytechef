
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

import static com.bytechef.hermes.component.RestComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipelinesActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("getPipelines")
        .display(
            display("Get all pipelines")
                .description("Returns data about all pipelines."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/pipelines"

            ))
        .properties()
        .output(object(null)
            .properties(array("data").items(object(null).properties(string("update_time").label("Update_time")
                .description("The pipeline update time. Format: YYYY-MM-DD HH:MM:SS.")
                .required(false),
                string("url_title").label("Url_title")
                    .description("The pipeline title displayed in the URL")
                    .required(false),
                integer("order_nr").label("Order_nr")
                    .description("Defines the order of pipelines. First order (`order_nr=0`) is the default pipeline.")
                    .required(false),
                string("name").label("Name")
                    .description("The name of the pipeline")
                    .required(false),
                bool("active").label("Active")
                    .description("Whether this pipeline will be made inactive (hidden) or active")
                    .required(false),
                bool("deal_probability").label("Deal_probability")
                    .description("Whether deal probability is disabled or enabled for this pipeline")
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the pipeline")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The pipeline creation time. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                bool("selected").label("Selected")
                    .description("A boolean that shows if the pipeline is selected from a filter or not")
                    .required(false))
                .description("Pipelines array"))
                .placeholder("Add")
                .label("Data")
                .description("Pipelines array")
                .required(false),
                bool("success").label("Success")
                    .description("If the response is successful or not")
                    .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"success\":true,\"data\":[{\"id\":1,\"name\":\"Pipeline Name\",\"url_title\":\"Pipeline-Name\",\"order_nr\":1,\"active\":true,\"deal_probability\":true,\"add_time\":\"2017-08-03 12:51:18\",\"update_time\":\"2020-03-23 13:15:25\",\"selected\":true}]}"),
        action("getPipeline")
            .display(
                display("Get one pipeline")
                    .description(
                        "Returns data about a specific pipeline. Also returns the summary of the deals in this pipeline across its stages."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/pipelines/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the pipeline")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
                string("totals_convert_currency").label("Totals_convert_currency")
                    .description(
                        "The 3-letter currency code of any of the supported currencies. When supplied, `per_stages_converted` is returned in `deals_summary` which contains the currency-converted total amounts in the given currency per each stage. You may also set this parameter to `default_currency` in which case users default currency is used.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)))
            .output(object(null).properties(string("update_time").label("Update_time")
                .description("The pipeline update time. Format: YYYY-MM-DD HH:MM:SS.")
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
                string("url_title").label("Url_title")
                    .description("The pipeline title displayed in the URL")
                    .required(false),
                integer("order_nr").label("Order_nr")
                    .description("Defines the order of pipelines. First order (`order_nr=0`) is the default pipeline.")
                    .required(false),
                string("name").label("Name")
                    .description("The name of the pipeline")
                    .required(false),
                bool("active").label("Active")
                    .description("Whether this pipeline will be made inactive (hidden) or active")
                    .required(false),
                bool("deal_probability").label("Deal_probability")
                    .description("Whether deal probability is disabled or enabled for this pipeline")
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the pipeline")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The pipeline creation time. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                bool("selected").label("Selected")
                    .description("A boolean that shows if the pipeline is selected from a filter or not")
                    .required(false),
                bool("success").label("Success")
                    .description("If the response is successful or not")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", ResponseFormat.JSON)))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":1,\"name\":\"Pipeline\",\"url_title\":\"Pipeline\",\"order_nr\":1,\"active\":true,\"deal_probability\":true,\"add_time\":\"2017-08-03 12:51:18\",\"update_time\":\"2020-03-23 13:15:25\",\"selected\":true,\"deals_summary\":{\"per_stages\":{\"1\":{\"EUR\":{\"count\":1,\"value\":10,\"value_formatted\":\"10 €\",\"weighted_value\":10,\"weighted_value_formatted\":\"10€\"}}},\"per_currency\":{\"EUR\":1},\"total_count\":1,\"per_currency_full\":{\"EUR\":{\"count\":1,\"value\":10}}}}}"));
}
