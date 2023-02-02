
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
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
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
public class FiltersActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("getFilters")
        .display(
            display("Get all filters")
                .description("Returns data about all filters."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/filters"

            ))
        .properties(string("type").label("Type")
            .description("The types of filters to fetch")
            .options(option("Deals", "deals"), option("Leads", "leads"), option("Org", "org"),
                option("People", "people"), option("Products", "products"), option("Activity", "activity"))
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(object(null).properties(array("data").items(object(null).properties(integer("id").label("Id")
            .description("The ID of the filter")
            .required(false),
            string("name").label("Name")
                .description("The name of the filter")
                .required(false),
            bool("active_flag").label("Active_flag")
                .description("The active flag of the filter")
                .required(false),
            string("type").label("Type")
                .description("The type of the item")
                .required(false),
            integer("user_id").label("User_id")
                .description("The owner of the filter")
                .required(false),
            string("add_time").label("Add_time")
                .description("The date and time when the filter was added")
                .required(false),
            string("update_time").label("Update_time")
                .description("The date and time when the filter was updated")
                .required(false),
            integer("visible_to").label("Visible_to")
                .description("The visibility group ID of who can see then filter")
                .required(false),
            integer("custom_view_id").label("Custom_view_id")
                .description("Used by Pipedrive webapp")
                .required(false))
            .description("The array of filters"))
            .placeholder("Add")
            .label("Data")
            .description("The array of filters")
            .required(false),
            bool("success").label("Success")
                .description("If the response is successful or not")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"success\":true,\"data\":[{\"id\":1,\"name\":\"All open deals\",\"active_flag\":true,\"type\":\"deals\",\"temporary_flag\":null,\"user_id\":927097,\"add_time\":\"2019-10-15 11:01:53\",\"update_time\":\"2019-10-15 11:01:53\",\"visible_to\":7,\"custom_view_id\":1}]}"));
}
