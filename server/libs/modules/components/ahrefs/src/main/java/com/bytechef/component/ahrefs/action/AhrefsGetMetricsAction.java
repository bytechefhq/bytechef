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

package com.bytechef.component.ahrefs.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AhrefsGetMetricsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getMetrics")
        .title("Get Metrics")
        .description("Returns metrics from target.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/site-explorer/metrics"

            ))
        .properties(string("target").label("Target")
            .description("The target of the search: a domain or a URL.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("date").label("Date")
                .description("A date to report metrics on in YYYY-MM-DD format.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("volume_mode").label("Volume Mode")
                .description(
                    "The search volume calculation mode: monthly or average. It affects volume, traffic, and traffic value.")
                .options(option("Monthly", "monthly"), option("Average", "average"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("protocol").label("Protocol")
                .description("The protocol of your target")
                .options(option("Both", "both"), option("Http", "http"), option("Https", "https"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("output").label("Output")
                .description("The output format.")
                .options(option("Json", "json"), option("Csv", "csv"), option("Xml", "xml"), option("Php", "php"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("mode").label("Mode")
                .description(
                    "The search volume calculation mode: monthly or average. It affects volume, traffic, and traffic value.")
                .options(option("Exact", "exact"), option("Prefix", "prefix"), option("Domain", "domain"),
                    option("Subdomains", "subdomains"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("metrics").properties(
                integer("org_keywords").description(
                    "The total number of keywords that your target ranks for in the top 100 organic search results.")
                    .required(false),
                integer("paid_keywords")
                    .description("The total number of keywords that your target ranks for in paid search results.")
                    .required(false),
                integer("org_keywords_1_3")
                    .description(
                        "The total number of keywords that your target ranks for in the top 3 organic search results.")
                    .required(false),
                integer("org_traffic").description(
                    "(10 units) The estimated number of monthly visitors that your target gets from organic search.")
                    .required(false),
                integer("org_cost")
                    .description(
                        "(10 units) The estimated value of your target's monthly organic search traffic, in USD cents.")
                    .required(false),
                integer("paid_traffic")
                    .description(
                        "(10 units) The estimated number of monthly visitors that your target gets from paid search.")
                    .required(false),
                integer("paid_cost")
                    .description(
                        "(10 units) The estimated cost of your target's monthly paid search traffic, in USD cents.")
                    .required(false),
                integer("paid_pages")
                    .description("The total number of pages from a target ranking in paid search results.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AhrefsGetMetricsAction() {
    }
}
