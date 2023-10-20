
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

package com.bytechef.component.shopify.action;

import static com.bytechef.hermes.component.RestComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ProductsActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("get_smart_collections")
        .display(
            display(
                "Retrieves a list of smart collections. Note: As of version 2019-10, this endpoint implements pagination by using links that are provided in the response header. To learn more, see Making requests to paginated REST Admin API endpoints.")
                    .description(
                        "https://shopify.dev/docs/admin-api/rest/reference/products/smartcollection#index-2020-10"))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/admin/api/2020-10/smart_collections.json"

            ))
        .properties(object("limit").label("Limit")
            .description("The number of results to show.\n"
                + "                  (default: 50, maximum: 250)")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            object("ids").label("Ids")
                .description("Show only the smart collections specified by a comma-separated list of IDs.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("since_id").label("Since_id")
                .description("Restrict results to after the specified ID.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("title").label("Title")
                .description("Show smart collections with the specified title.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("product_id").label("Product_id")
                .description("Show smart collections that includes the specified product.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("handle").label("Handle")
                .description("Filter results by smart collection handle.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("updated_at_min").label("Updated_at_min")
                .description("Show smart collections last updated after this date. (format: 2014-04-25T16:15:47-04:00)")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("updated_at_max").label("Updated_at_max")
                .description(
                    "Show smart collections last updated before this date. (format: 2014-04-25T16:15:47-04:00)")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("published_at_min").label("Published_at_min")
                .description("Show smart collections published after this date. (format: 2014-04-25T16:15:47-04:00)")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("published_at_max").label("Published_at_max")
                .description("Show smart collections published before this date. (format: 2014-04-25T16:15:47-04:00)")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("published_status").label("Published_status")
                .description("Filter results based on the published status of smart collections.\n"
                    + "                  (default: any)\n"
                    + "                    \n"
                    + "                        published: Show only published smart collections.\n"
                    + "                        unpublished: Show only unpublished smart collections.\n"
                    + "                        any: Show all smart collections.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("fields").label("Fields")
                .description("Show only certain fields, specified by a comma-separated list of field names.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY))),
        action("get_smart_collections_count")
            .display(
                display("Retrieves a count of smart collections")
                    .description(
                        "https://shopify.dev/docs/admin-api/rest/reference/products/smartcollection#count-2020-10"))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/admin/api/2020-10/smart_collections/count.json"

                ))
            .properties(object("title").label("Title")
                .description("Show smart collections with the specified title.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
                object("product_id").label("Product_id")
                    .description("Show smart collections that include the specified product.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                object("updated_at_min").label("Updated_at_min")
                    .description(
                        "Show smart collections last updated after this date. (format: 2014-04-25T16:15:47-04:00)")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                object("updated_at_max").label("Updated_at_max")
                    .description(
                        "Show smart collections last updated before this date.  (format: 2014-04-25T16:15:47-04:00)")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                object("published_at_min").label("Published_at_min")
                    .description(
                        "Show smart collections published after this date.  (format: 2014-04-25T16:15:47-04:00)")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                object("published_at_max").label("Published_at_max")
                    .description(
                        "Show smart collections published before this date.  (format: 2014-04-25T16:15:47-04:00)")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                object("published_status").label("Published_status")
                    .description("Filter results based on the published status of smart collections.\n"
                        + "                  (default: any)\n"
                        + "                    \n"
                        + "                        published: Show only published smart collections.\n"
                        + "                        unpublished: Show only unpublished smart collections.\n"
                        + "                        any: Show all smart collections.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY))));
}
