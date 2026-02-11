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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveGetDealsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getDeals")
        .title("Get Deals")
        .description("Returns all deals.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/deals"

            ))
        .properties(integer("user_id").label("User ID")
            .description(
                "Deals matching the given user will be returned. However, `filter_id` and `owned_by_you` takes precedence over `user_id` when supplied.")
            .required(false)
            .options((ActionDefinition.OptionsFunction<Long>) PipedriveUtils::getUserIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            integer("filter_id").label("Filter ID")
                .description("ID of the filter to use.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<Long>) PipedriveUtils::getFilterIdOptions)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("stage_id").label("Stage ID")
                .description("Deals within the given stage will be returned.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<Long>) PipedriveUtils::getStageIdOptions)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("status").label("Status")
                .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"),
                    option("Deleted", "deleted"), option("All_not_deleted", "all_not_deleted"))
                .defaultValue("all_not_deleted")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("sort").label("Sort")
                .description(
                    "The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(array("data")
                .items(object().properties(integer("id").required(false),
                    object("user_id")
                        .properties(integer("id").required(false), string("name").required(false),
                            string("email").required(false))
                        .required(false),
                    object("person_id").properties(string("name").required(false))
                        .required(false),
                    object("org_id").properties(string("name").required(false), string("owner_id").required(false))
                        .required(false),
                    integer("stage_id").required(false), string("title").required(false),
                    integer("value").required(false), string("currency").required(false),
                    string("status").required(false)))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipedriveGetDealsAction() {
    }
}
