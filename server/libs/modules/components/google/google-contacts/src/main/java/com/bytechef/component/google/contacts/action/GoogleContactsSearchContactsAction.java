/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CONTACT_LIST_OUTPUT_PROPERTY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.QUERY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.SearchResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Erhan Tunçel
 */
public class GoogleContactsSearchContactsAction {

    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("searchContacts")
        .title("Search Contacts")
        .description("Searches the contacts.")
        .properties(
            string(QUERY)
                .label("Query")
                .description("The plain-text query.")
                .required(true))
        .output(outputSchema(CONTACT_LIST_OUTPUT_PROPERTY))
        .perform(GoogleContactsSearchContactsAction::perform);

    private GoogleContactsSearchContactsAction() {

    }

    protected static List<Person> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws IOException, InterruptedException {

        PeopleService peopleService = GoogleServices.getPeopleService(connectionParameters);
        String fieldMasks = "names,nicknames,emailAddresses,phoneNumbers,organizations";

        // Warmup cache
        peopleService
            .people()
            .searchContacts()
            .setQuery("")
            .setReadMask(fieldMasks)
            .execute();

        // Wait a few seconds
        Thread.sleep(5);

        SearchResponse searchResponse = peopleService
            .people()
            .searchContacts()
            .setQuery(inputParameters.getString(QUERY))
            .setReadMask(fieldMasks)
            .execute();

        List<Person> personList = new ArrayList<>();
        Optional
            .ofNullable(searchResponse.getResults())
            .orElse(Collections.emptyList())
            .forEach(searchResult -> personList.add(searchResult.getPerson()));

        return personList;
    }
}
