
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

public class ActivityTypesActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(
        action("getActivityTypes")
            .display(display("Get all activity types").description("Returns all activity types."))
            .metadata(Map.of("requestMethod", "GET", "path", "/activityTypes"))
            .properties()
            .output(object(null)
                .properties(
                    array("data")
                        .items(object(null)
                            .properties(
                                integer("id")
                                    .label("Id")
                                    .description("The ID of the activity type")
                                    .required(false),
                                string("name")
                                    .label("Name")
                                    .description("The name of the activity type")
                                    .required(false),
                                string("icon_key")
                                    .label("Icon_key")
                                    .description(
                                        "Icon graphic to use for representing this activity type")
                                    .options(
                                        option("Task", "task"),
                                        option("Email", "email"),
                                        option("Meeting", "meeting"),
                                        option("Deadline", "deadline"),
                                        option("Call", "call"),
                                        option("Lunch", "lunch"),
                                        option("Calendar", "calendar"),
                                        option("Downarrow", "downarrow"),
                                        option("Document", "document"),
                                        option("Smartphone", "smartphone"),
                                        option("Camera", "camera"),
                                        option("Scissors", "scissors"),
                                        option("Cogs", "cogs"),
                                        option("Bubble", "bubble"),
                                        option("Uparrow", "uparrow"),
                                        option("Checkbox", "checkbox"),
                                        option("Signpost", "signpost"),
                                        option("Shuffle", "shuffle"),
                                        option("Addressbook", "addressbook"),
                                        option("Linegraph", "linegraph"),
                                        option("Picture", "picture"),
                                        option("Car", "car"),
                                        option("World", "world"),
                                        option("Search", "search"),
                                        option("Clip", "clip"),
                                        option("Sound", "sound"),
                                        option("Brush", "brush"),
                                        option("Key", "key"),
                                        option("Padlock", "padlock"),
                                        option("Pricetag", "pricetag"),
                                        option("Suitcase", "suitcase"),
                                        option("Finish", "finish"),
                                        option("Plane", "plane"),
                                        option("Loop", "loop"),
                                        option("Wifi", "wifi"),
                                        option("Truck", "truck"),
                                        option("Cart", "cart"),
                                        option("Bulb", "bulb"),
                                        option("Bell", "bell"),
                                        option("Presentation", "presentation"))
                                    .required(false),
                                string("color")
                                    .label("Color")
                                    .description(
                                        "A designated color for the activity type in 6-character HEX format (e.g. `FFFFFF` for white, `000000` for black)")
                                    .required(false),
                                integer("order_nr")
                                    .label("Order_nr")
                                    .description(
                                        "An order number for the activity type. Order numbers should be used to order the types in the activity type selections.")
                                    .required(false),
                                string("key_string")
                                    .label("Key_string")
                                    .description(
                                        "A string that is generated by the API based on the given name of the activity type upon creation")
                                    .required(false),
                                bool("active_flag")
                                    .label("Active_flag")
                                    .description("The active flag of the activity type")
                                    .required(false),
                                bool("is_custom_flag")
                                    .label("Is_custom_flag")
                                    .description(
                                        "Whether the activity type is a custom one or not")
                                    .required(false),
                                string("add_time")
                                    .label("Add_time")
                                    .description(
                                        "The creation time of the activity type")
                                    .required(false),
                                string("update_time")
                                    .label("Update_time")
                                    .description("The update time of the activity type")
                                    .required(false))
                            .description("The array of activity types"))
                        .label("Data")
                        .description("The array of activity types")
                        .required(false),
                    bool("success")
                        .label("Success")
                        .description("If the response is successful or not")
                        .required(false))
                .metadata(Map.of("responseType", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":4,\"order_nr\":1,\"name\":\"Deadline\",\"key_string\":\"deadline\",\"icon_key\":\"deadline\",\"active_flag\":true,\"color\":\"FFFFFF\",\"is_custom_flag\":false,\"add_time\":\"2019-10-04 16:24:55\",\"update_time\":\"2020-03-11 13:53:01\"},{\"id\":5,\"order_nr\":2,\"name\":\"Call\",\"key_string\":\"call\",\"icon_key\":\"call\",\"active_flag\":true,\"color\":\"FFFFFF\",\"is_custom_flag\":false,\"add_time\":\"2019-12-21 19:44:01\",\"update_time\":\"2019-12-21 19:44:01\"}]}"));
}
