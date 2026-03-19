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

package com.bytechef.component.google.contacts.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PAGE_SIZE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.QUERY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.READ_MASK;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Erhan Tunçel
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
public class GoogleContactsSearchContactsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchContacts")
        .title("Search Contacts")
        .description("Searches the contacts in Google Contacts account.")
        .properties(
            string(QUERY)
                .label("Query")
                .description(
                    "The plain-text query for the request.The query is used to match prefix phrases of the fields " +
                        "on a person. For example, a person with name \"foo name\" matches queries such as \"f\", " +
                        "\"fo\", \"foo\", \"foo n\", \"nam\", etc., but not \"oo n\".")
                .required(true),
            array(READ_MASK)
                .label("Read Mask")
                .description("A field mask to restrict which fields on each person are returned.")
                .items(string())
                .options(List.of(
                    option("Addresses", "addresses"),
                    option("Age Ranges", "ageRanges"),
                    option("Biographies", "biographies"),
                    option("Birthdays", "birthdays"),
                    option("Calendar Urls", "calendarUrls"),
                    option("Client Data", "clientData"),
                    option("Cover Photos", "coverPhotos"),
                    option("Email Addresses", "emailAddresses"),
                    option("Events", "events"),
                    option("External Ids", "externalIds"),
                    option("Genders", "genders"),
                    option("Im Clients", "imClients"),
                    option("Interests", "interests"),
                    option("Locales", "locales"),
                    option("Locations", "locations"),
                    option("Memberships", "memberships"),
                    option("Metadata", "metadata"),
                    option("Misc Keywords", "miscKeywords"),
                    option("Names", "names"),
                    option("Nicknames", "nicknames"),
                    option("Occupations", "occupations"),
                    option("Organizations", "organizations"),
                    option("Phone Numbers", "phoneNumbers"),
                    option("Photos", "photos"),
                    option("Relations", "relations"),
                    option("Sip Addresses", "sipAddresses"),
                    option("Skills", "skills"),
                    option("Urls", "urls"),
                    option("User Defined", "userDefined")))
                .defaultValue("names", "emailAddresses")
                .required(true),
            integer(PAGE_SIZE)
                .label("Page Size")
                .description("The number of results to return per page.")
                .defaultValue(10)
                .maxValue(30)
                .required(false))
        .output()
        .perform(GoogleContactsSearchContactsAction::perform);

    private GoogleContactsSearchContactsAction() {
    }

    public static List<Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, List<Object>> response = context.http(http -> http.get("/people:searchContacts"))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameters(
                QUERY, inputParameters.getRequiredString(QUERY),
                PAGE_SIZE, inputParameters.getInteger(PAGE_SIZE),
                READ_MASK, String.join(",", inputParameters.getRequiredList(READ_MASK, String.class)))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get("results");
    }
}
