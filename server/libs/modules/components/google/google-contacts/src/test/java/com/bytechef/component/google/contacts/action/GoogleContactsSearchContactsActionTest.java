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

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.SearchResponse;
import com.google.api.services.people.v1.model.SearchResult;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Erhan Tunçel
 */
public class GoogleContactsSearchContactsActionTest extends AbstractGoogleContactsActionTest {

    private final PeopleService.People mockedPeople = mock(PeopleService.People.class);
    private final PeopleService.People.SearchContacts mockedSearchContacts =
        mock(PeopleService.People.SearchContacts.class);
    private final ArgumentCaptor<String> queryArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final SearchResponse mockedSearchResponse = mock(SearchResponse.class);

    @Test
    void testPerform() throws IOException, InterruptedException {

        Person person = new Person()
            .setNames(List.of(new Name().setGivenName("Name")))
            .setEmailAddresses(List.of(new EmailAddress().setValue("name@localhost.com")));
        SearchResult searchResult = new SearchResult();
        searchResult.setPerson(person);

        when(mockedParameters.getRequiredString(QUERY))
            .thenReturn("Name");

        when(mockedPeopleService.people())
            .thenReturn(mockedPeople);
        when(mockedPeople.searchContacts())
            .thenReturn(mockedSearchContacts);
        when(mockedSearchContacts.setQuery(queryArgumentCaptor.capture()))
            .thenReturn(mockedSearchContacts);
        when(mockedSearchContacts.setQuery(anyString()))
            .thenReturn(mockedSearchContacts);
        when(mockedSearchContacts.setReadMask(anyString()))
            .thenReturn(mockedSearchContacts);
        when(mockedSearchContacts.execute())
            .thenReturn(mockedSearchResponse);
        when(mockedSearchResponse.getResults())
            .thenReturn(List.of(searchResult));

        List<Person> personList = GoogleContactsSearchContactsAction
            .perform(mockedParameters, mockedParameters, mockedContext);

        assertNotNull(personList);
        assertEquals(1, personList.size());
        Person firstPerson = personList.getFirst();
        assertEquals("Name", firstPerson.getNames()
            .getFirst()
            .getGivenName());
        assertEquals("name@localhost.com", firstPerson.getEmailAddresses()
            .getFirst()
            .getValue());
    }
}
