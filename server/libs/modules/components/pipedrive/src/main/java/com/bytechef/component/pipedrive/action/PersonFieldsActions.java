
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
public class PersonFieldsActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("getPersonFields")
        .display(
            display("Get all person fields")
                .description(
                    "Returns data about all person fields.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also return the `data.marketing_status` field."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/personFields"

            ))
        .properties(integer("start").label("Start")
            .description("Pagination start")
            .required(false)
            .metadata(
                Map.of(
                    "type", "QUERY")),
            integer("limit").label("Limit")
                .description("Items shown per page")
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")))
        .output(object(null).properties(array("data").items(object(null).properties(integer("id").label("Id")
            .description("The ID of the field. Value is `null` in case of subfields.")
            .required(false),
            string("key").label("Key")
                .description("The key of the field. For custom fields this is generated upon creation.")
                .required(false),
            string("name").label("Name")
                .description("The name of the field")
                .required(false),
            integer("order_nr").label("Order_nr")
                .description("The order number of the field")
                .required(false),
            string("field_type").label("Field_type")
                .options(option("Address", "address"), option("Date", "date"), option("Daterange", "daterange"),
                    option("Double", "double"), option("Enum", "enum"), option("Monetary", "monetary"),
                    option("Org", "org"), option("People", "people"), option("Phone", "phone"), option("Set", "set"),
                    option("Text", "text"), option("Time", "time"), option("Timerange", "timerange"),
                    option("User", "user"), option("Varchar", "varchar"), option("Varchar_auto", "varchar_auto"),
                    option("Visible_to", "visible_to"))
                .required(false),
            string("add_time").label("Add_time")
                .description("The creation time of the field")
                .required(false),
            string("update_time").label("Update_time")
                .description("The update time of the field")
                .required(false),
            integer("last_updated_by_user_id").label("Last_updated_by_user_id")
                .description(
                    "The ID of the user who created or most recently updated the field, only applicable for custom fields")
                .required(false),
            bool("active_flag").label("Active_flag")
                .description("The active flag of the field")
                .required(false),
            bool("edit_flag").label("Edit_flag")
                .description("The edit flag of the field")
                .required(false),
            bool("index_visible_flag").label("Index_visible_flag")
                .description("Not used")
                .required(false),
            bool("details_visible_flag").label("Details_visible_flag")
                .description("Not used")
                .required(false),
            bool("add_visible_flag").label("Add_visible_flag")
                .description("Not used")
                .required(false),
            bool("important_flag").label("Important_flag")
                .description("Not used")
                .required(false),
            bool("bulk_edit_allowed").label("Bulk_edit_allowed")
                .description("Whether or not the field of an item can be edited in bulk")
                .required(false),
            bool("searchable_flag").label("Searchable_flag")
                .description("Whether or not items can be searched by this field")
                .required(false),
            bool("filtering_allowed").label("Filtering_allowed")
                .description("Whether or not items can be filtered by this field")
                .required(false),
            bool("sortable_flag").label("Sortable_flag")
                .description("Whether or not items can be sorted by this field")
                .required(false),
            bool("mandatory_flag").label("Mandatory_flag")
                .description("Whether or not the field is mandatory")
                .required(false),
            array("options")
                .items(object(null)
                    .description("The options of the field. When there are no options, `null` is returned."))
                .placeholder("Add")
                .label("Options")
                .description("The options of the field. When there are no options, `null` is returned.")
                .required(false),
            array("options_deleted")
                .items(object(null).description(
                    "The deleted options of the field. Only present when there is at least 1 deleted option."))
                .placeholder("Add")
                .label("Options_deleted")
                .description("The deleted options of the field. Only present when there is at least 1 deleted option.")
                .required(false),
            bool("is_subfield").label("Is_subfield")
                .description(
                    "Whether or not the field is a subfield of another field. Only present if field is subfield.")
                .required(false),
            array("subfields")
                .items(
                    object(null).description("The subfields of the field. Only present when the field has subfields."))
                .placeholder("Add")
                .label("Subfields")
                .description("The subfields of the field. Only present when the field has subfields.")
                .required(false)))
            .placeholder("Add")
            .label("Data")
            .required(false),
            bool("success").label("Success")
                .description("If the response is successful or not")
                .required(false),
            object("additional_data").properties(integer("start").label("Start")
                .description("Pagination start")
                .required(false),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false),
                bool("more_items_in_collection").label("More_items_in_collection")
                    .description("If there are more list items in the collection than displayed or not")
                    .required(false))
                .label("Additional_data")
                .description("The additional data of the list")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", "JSON")))
        .exampleOutput(
            "{\"success\":true,\"data\":[{\"id\":1,\"key\":\"title\",\"name\":\"Title\",\"order_nr\":2,\"field_type\":\"varchar\",\"add_time\":\"2019-02-04 13:58:03\",\"update_time\":\"2019-02-04 13:58:03\",\"last_updated_by_user_id\":1,\"active_flag\":true,\"edit_flag\":false,\"index_visible_flag\":true,\"details_visible_flag\":true,\"add_visible_flag\":true,\"important_flag\":false,\"bulk_edit_allowed\":true,\"searchable_flag\":false,\"filtering_allowed\":true,\"sortable_flag\":true,\"options\":null,\"mandatory_flag\":true},{\"id\":2,\"key\":\"9dc80c50d78a15643bfc4ca79d76156a73a1ca0e\",\"name\":\"Customer Type\",\"order_nr\":1,\"field_type\":\"enum\",\"add_time\":\"2019-02-04 13:58:03\",\"update_time\":\"2019-02-04 13:58:03\",\"last_updated_by_user_id\":1,\"active_flag\":true,\"edit_flag\":true,\"index_visible_flag\":true,\"details_visible_flag\":true,\"add_visible_flag\":false,\"important_flag\":false,\"bulk_edit_allowed\":true,\"searchable_flag\":false,\"filtering_allowed\":true,\"sortable_flag\":true,\"options\":[{\"id\":190,\"label\":\"Private person\"},{\"id\":191,\"label\":\"Company\"},{\"id\":192,\"label\":\"Government\"}],\"mandatory_flag\":true}],\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}"));
}
