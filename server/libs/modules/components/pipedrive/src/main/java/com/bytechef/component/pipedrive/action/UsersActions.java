
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

public class UsersActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(
        action("getUsers")
            .display(display("Get all users").description("Returns data about all users within the company."))
            .metadata(Map.of("requestMethod", "GET", "path", "/users"))
            .properties()
            .output(object(null)
                .properties(
                    array("data")
                        .items(object(null)
                            .properties(
                                integer("id")
                                    .label("Id")
                                    .description("The user ID")
                                    .required(false),
                                string("name")
                                    .label("Name")
                                    .description("The user name")
                                    .required(false),
                                string("default_currency")
                                    .label("Default_currency")
                                    .description("The user default currency")
                                    .required(false),
                                string("locale")
                                    .label("Locale")
                                    .description("The user locale")
                                    .required(false),
                                integer("lang")
                                    .label("Lang")
                                    .description("The user language ID")
                                    .required(false),
                                string("email")
                                    .label("Email")
                                    .description("The user email")
                                    .required(false),
                                string("phone")
                                    .label("Phone")
                                    .description("The user phone")
                                    .required(false),
                                bool("activated")
                                    .label("Activated")
                                    .description(
                                        "Boolean that indicates whether the user is activated")
                                    .required(false),
                                string("last_login")
                                    .label("Last_login")
                                    .description(
                                        "The last login date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                    .required(false),
                                string("created")
                                    .label("Created")
                                    .description(
                                        "The creation date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                    .required(false),
                                string("modified")
                                    .label("Modified")
                                    .description(
                                        "The last modification date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                    .required(false),
                                bool("has_created_company")
                                    .label("Has_created_company")
                                    .description(
                                        "Boolean that indicates whether the user has created a company")
                                    .required(false),
                                array("access")
                                    .items(object(null)
                                        .properties(
                                            string("app")
                                                .label("App")
                                                .options(
                                                    option(
                                                        "Sales",
                                                        "sales"),
                                                    option(
                                                        "Projects",
                                                        "projects"),
                                                    option(
                                                        "Campaigns",
                                                        "campaigns"),
                                                    option(
                                                        "Global",
                                                        "global"),
                                                    option(
                                                        "Account_settings",
                                                        "account_settings"))
                                                .required(false),
                                            bool("admin")
                                                .label("Admin")
                                                .required(false),
                                            string("permission_set_id")
                                                .label("Permission_set_id")
                                                .required(false)))
                                    .label("Access")
                                    .required(false),
                                bool("active_flag")
                                    .label("Active_flag")
                                    .description(
                                        "Boolean that indicates whether the user is activated")
                                    .required(false),
                                string("timezone_name")
                                    .label("Timezone_name")
                                    .description("The user timezone name")
                                    .required(false),
                                string("timezone_offset")
                                    .label("Timezone_offset")
                                    .description("The user timezone offset")
                                    .required(false),
                                integer("role_id")
                                    .label("Role_id")
                                    .description("The ID of the user role")
                                    .required(false),
                                string("icon_url")
                                    .label("Icon_url")
                                    .description("The user icon URL")
                                    .required(false),
                                bool("is_you")
                                    .label("Is_you")
                                    .description(
                                        "Boolean that indicates if the requested user is the same which is logged in (in this case, always true)")
                                    .required(false)))
                        .label("Data")
                        .required(false),
                    bool("success")
                        .label("Success")
                        .description("If the response is successful or not")
                        .required(false))
                .metadata(Map.of("responseType", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":1,\"name\":\"John Doe\",\"default_currency\":\"EUR\",\"locale\":\"et_EE\",\"lang\":1,\"email\":\"john@pipedrive.com\",\"phone\":\"0000-0001\",\"activated\":true,\"last_login\":\"2019-11-21 08:45:56\",\"created\":\"2018-11-13 09:16:26\",\"modified\":\"2019-11-21 08:45:56\",\"has_created_company\":true,\"access\":[{\"app\":\"sales\",\"admin\":true,\"permission_set_id\":\"62cc4d7f-4038-4352-abf3-a8c1c822b631\"},{\"app\":\"global\",\"admin\":true,\"permission_set_id\":\"233b7976-39bd-43a9-b305-ef3a2b0998e5\"},{\"app\":\"account_settings\",\"admin\":true,\"permission_set_id\":\"982c5ce5-b8ba-4b47-b102-9da024f4b990\"}],\"active_flag\":true,\"timezone_name\":\"Europe/Berlin\",\"timezone_offset\":\"+03:00\",\"role_id\":1,\"icon_url\":\"https://upload.wikimedia.org/wikipedia/en/thumb/e/e0/WPVG_icon_2016.svg/1024px-WPVG_icon_2016.svg.png\",\"is_you\":true},{\"id\":2,\"name\":\"Jane Doe\",\"default_currency\":\"EUR\",\"locale\":\"et_EE\",\"lang\":1,\"email\":\"jane@pipedrive.com\",\"phone\":\"0000-0002\",\"activated\":true,\"last_login\":\"2019-09-11 11:43:54\",\"created\":\"2019-01-22 10:43:47\",\"modified\":\"2019-11-21 09:49:50\",\"has_created_company\":false,\"access\":[{\"app\":\"sales\",\"admin\":false,\"permission_set_id\":\"f07d229d-088a-4144-a40f-1fe64295d180\"},{\"app\":\"global\",\"admin\":true,\"permission_set_id\":\"233b7976-39bd-43a9-b305-ef3a2b0998e5\"}],\"active_flag\":true,\"timezone_name\":\"Europe/Berlin\",\"timezone_offset\":\"+03:00\",\"role_id\":1,\"icon_url\":null,\"is_you\":false}]}"),
        action("findUsersByName")
            .display(display("Find users by name").description("Finds users by their name."))
            .metadata(Map.of("requestMethod", "GET", "path", "/users/find"))
            .properties(
                string("term")
                    .label("Term")
                    .description("The search term to look for")
                    .required(true)
                    .metadata(Map.of("type", "QUERY")),
                number("search_by_email")
                    .label("Search_by_email")
                    .description(
                        "When enabled, the term will only be matched against email addresses of users. Default: `false`")
                    .options(option("0", 0), option("1", 1))
                    .required(false)
                    .metadata(Map.of("type", "QUERY")))
            .output(object(null)
                .properties(
                    array("data")
                        .items(object(null)
                            .properties(
                                integer("id")
                                    .label("Id")
                                    .description("The user ID")
                                    .required(false),
                                string("name")
                                    .label("Name")
                                    .description("The user name")
                                    .required(false),
                                string("default_currency")
                                    .label("Default_currency")
                                    .description("The user default currency")
                                    .required(false),
                                string("locale")
                                    .label("Locale")
                                    .description("The user locale")
                                    .required(false),
                                integer("lang")
                                    .label("Lang")
                                    .description("The user language ID")
                                    .required(false),
                                string("email")
                                    .label("Email")
                                    .description("The user email")
                                    .required(false),
                                string("phone")
                                    .label("Phone")
                                    .description("The user phone")
                                    .required(false),
                                bool("activated")
                                    .label("Activated")
                                    .description(
                                        "Boolean that indicates whether the user is activated")
                                    .required(false),
                                string("last_login")
                                    .label("Last_login")
                                    .description(
                                        "The last login date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                    .required(false),
                                string("created")
                                    .label("Created")
                                    .description(
                                        "The creation date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                    .required(false),
                                string("modified")
                                    .label("Modified")
                                    .description(
                                        "The last modification date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                    .required(false),
                                bool("has_created_company")
                                    .label("Has_created_company")
                                    .description(
                                        "Boolean that indicates whether the user has created a company")
                                    .required(false),
                                array("access")
                                    .items(object(null)
                                        .properties(
                                            string("app")
                                                .label("App")
                                                .options(
                                                    option(
                                                        "Sales",
                                                        "sales"),
                                                    option(
                                                        "Projects",
                                                        "projects"),
                                                    option(
                                                        "Campaigns",
                                                        "campaigns"),
                                                    option(
                                                        "Global",
                                                        "global"),
                                                    option(
                                                        "Account_settings",
                                                        "account_settings"))
                                                .required(false),
                                            bool("admin")
                                                .label("Admin")
                                                .required(false),
                                            string("permission_set_id")
                                                .label("Permission_set_id")
                                                .required(false)))
                                    .label("Access")
                                    .required(false),
                                bool("active_flag")
                                    .label("Active_flag")
                                    .description(
                                        "Boolean that indicates whether the user is activated")
                                    .required(false),
                                string("timezone_name")
                                    .label("Timezone_name")
                                    .description("The user timezone name")
                                    .required(false),
                                string("timezone_offset")
                                    .label("Timezone_offset")
                                    .description("The user timezone offset")
                                    .required(false),
                                integer("role_id")
                                    .label("Role_id")
                                    .description("The ID of the user role")
                                    .required(false),
                                string("icon_url")
                                    .label("Icon_url")
                                    .description("The user icon URL")
                                    .required(false),
                                bool("is_you")
                                    .label("Is_you")
                                    .description(
                                        "Boolean that indicates if the requested user is the same which is logged in (in this case, always true)")
                                    .required(false)))
                        .label("Data")
                        .required(false),
                    bool("success")
                        .label("Success")
                        .description("If the response is successful or not")
                        .required(false))
                .metadata(Map.of("responseType", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":1,\"name\":\"John Doe\",\"default_currency\":\"EUR\",\"locale\":\"et_EE\",\"lang\":1,\"email\":\"john@pipedrive.com\",\"phone\":\"0000-0001\",\"activated\":true,\"last_login\":\"2019-11-21 08:45:56\",\"created\":\"2018-11-13 09:16:26\",\"modified\":\"2019-11-21 08:45:56\",\"has_created_company\":true,\"access\":[{\"app\":\"sales\",\"admin\":true,\"permission_set_id\":\"62cc4d7f-4038-4352-abf3-a8c1c822b631\"},{\"app\":\"global\",\"admin\":true,\"permission_set_id\":\"233b7976-39bd-43a9-b305-ef3a2b0998e5\"},{\"app\":\"account_settings\",\"admin\":true,\"permission_set_id\":\"982c5ce5-b8ba-4b47-b102-9da024f4b990\"}],\"active_flag\":true,\"timezone_name\":\"Europe/Berlin\",\"timezone_offset\":\"+03:00\",\"role_id\":1,\"icon_url\":\"https://upload.wikimedia.org/wikipedia/en/thumb/e/e0/WPVG_icon_2016.svg/1024px-WPVG_icon_2016.svg.png\",\"is_you\":true},{\"id\":2,\"name\":\"Jane Doe\",\"default_currency\":\"EUR\",\"locale\":\"et_EE\",\"lang\":1,\"email\":\"jane@pipedrive.com\",\"phone\":\"0000-0002\",\"activated\":true,\"last_login\":\"2019-09-11 11:43:54\",\"created\":\"2019-01-22 10:43:47\",\"modified\":\"2019-11-21 09:49:50\",\"has_created_company\":false,\"access\":[{\"app\":\"sales\",\"admin\":false,\"permission_set_id\":\"f07d229d-088a-4144-a40f-1fe64295d180\"},{\"app\":\"global\",\"admin\":true,\"permission_set_id\":\"233b7976-39bd-43a9-b305-ef3a2b0998e5\"}],\"active_flag\":true,\"timezone_name\":\"Europe/Berlin\",\"timezone_offset\":\"+03:00\",\"role_id\":1,\"icon_url\":null,\"is_you\":false}]}"),
        action("getUser")
            .display(display("Get one user")
                .description("Returns data about a specific user within the company."))
            .metadata(Map.of("requestMethod", "GET", "path", "/users/{id}"))
            .properties(integer("id")
                .label("Id")
                .description("The ID of the user")
                .required(true)
                .metadata(Map.of("type", "PATH")))
            .output(object(null)
                .properties(
                    object("data")
                        .properties(
                            integer("id")
                                .label("Id")
                                .description("The user ID")
                                .required(false),
                            string("name")
                                .label("Name")
                                .description("The user name")
                                .required(false),
                            string("default_currency")
                                .label("Default_currency")
                                .description("The user default currency")
                                .required(false),
                            string("locale")
                                .label("Locale")
                                .description("The user locale")
                                .required(false),
                            integer("lang")
                                .label("Lang")
                                .description("The user language ID")
                                .required(false),
                            string("email")
                                .label("Email")
                                .description("The user email")
                                .required(false),
                            string("phone")
                                .label("Phone")
                                .description("The user phone")
                                .required(false),
                            bool("activated")
                                .label("Activated")
                                .description(
                                    "Boolean that indicates whether the user is activated")
                                .required(false),
                            string("last_login")
                                .label("Last_login")
                                .description(
                                    "The last login date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                .required(false),
                            string("created")
                                .label("Created")
                                .description(
                                    "The creation date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                .required(false),
                            string("modified")
                                .label("Modified")
                                .description(
                                    "The last modification date and time of the user. Format: YYYY-MM-DD HH:MM:SS")
                                .required(false),
                            bool("has_created_company")
                                .label("Has_created_company")
                                .description(
                                    "Boolean that indicates whether the user has created a company")
                                .required(false),
                            array("access")
                                .items(object(null)
                                    .properties(
                                        string("app")
                                            .label("App")
                                            .options(
                                                option("Sales", "sales"),
                                                option(
                                                    "Projects",
                                                    "projects"),
                                                option(
                                                    "Campaigns",
                                                    "campaigns"),
                                                option("Global", "global"),
                                                option(
                                                    "Account_settings",
                                                    "account_settings"))
                                            .required(false),
                                        bool("admin")
                                            .label("Admin")
                                            .required(false),
                                        string("permission_set_id")
                                            .label("Permission_set_id")
                                            .required(false)))
                                .label("Access")
                                .required(false),
                            bool("active_flag")
                                .label("Active_flag")
                                .description(
                                    "Boolean that indicates whether the user is activated")
                                .required(false),
                            string("timezone_name")
                                .label("Timezone_name")
                                .description("The user timezone name")
                                .required(false),
                            string("timezone_offset")
                                .label("Timezone_offset")
                                .description("The user timezone offset")
                                .required(false),
                            integer("role_id")
                                .label("Role_id")
                                .description("The ID of the user role")
                                .required(false),
                            string("icon_url")
                                .label("Icon_url")
                                .description("The user icon URL")
                                .required(false),
                            bool("is_you")
                                .label("Is_you")
                                .description(
                                    "Boolean that indicates if the requested user is the same which is logged in (in this case, always true)")
                                .required(false))
                        .label("Data")
                        .required(false),
                    bool("success")
                        .label("Success")
                        .description("If the response is successful or not")
                        .required(false))
                .metadata(Map.of("responseType", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":2,\"name\":\"Jane Doe\",\"default_currency\":\"EUR\",\"locale\":\"et_EE\",\"lang\":1,\"email\":\"jane@pipedrive.com\",\"phone\":\"0000-0002\",\"activated\":true,\"last_login\":\"2019-09-11 11:43:54\",\"created\":\"2019-01-22 10:43:47\",\"modified\":\"2019-11-21 09:49:50\",\"has_created_company\":false,\"access\":[{\"app\":\"sales\",\"admin\":false,\"permission_set_id\":\"f07d229d-088a-4144-a40f-1fe64295d180\"},{\"app\":\"global\",\"admin\":true,\"permission_set_id\":\"233b7976-39bd-43a9-b305-ef3a2b0998e5\"}],\"active_flag\":true,\"timezone_name\":\"Europe/Berlin\",\"timezone_offset\":\"+03:00\",\"role_id\":1,\"icon_url\":null,\"is_you\":false}}"));
}
