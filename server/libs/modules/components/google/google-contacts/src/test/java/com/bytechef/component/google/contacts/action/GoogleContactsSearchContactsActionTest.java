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

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PAGE_SIZE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.QUERY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.READ_MASK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService.People;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.SearchResponse;
import com.google.api.services.people.v1.model.SearchResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Erhan Tunçel
 * @author Monika Kušter
 */
class GoogleContactsSearchContactsActionTest extends AbstractGoogleContactsActionTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final People mockedPeople = mock(People.class);
    private final People.SearchContacts mockedSearchContacts = mock(People.SearchContacts.class);
    private final SearchResponse mockedSearchResponse = mock(SearchResponse.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        mockedParameters = MockParametersFactory.create(
            Map.of(QUERY, "Name", READ_MASK, List.of("names", "organizations"), PAGE_SIZE, 5));

        Person person = new Person()
            .setNames(List.of(new Name().setGivenName("Name")))
            .setEmailAddresses(List.of(new EmailAddress().setValue("name@localhost.com")));
        SearchResult searchResult = new SearchResult();
        searchResult.setPerson(person);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getPeopleService(mockedParameters))
                .thenReturn(mockedPeopleService);

            when(mockedPeopleService.people())
                .thenReturn(mockedPeople);
            when(mockedPeople.searchContacts())
                .thenReturn(mockedSearchContacts);
            when(mockedSearchContacts.setQuery(stringArgumentCaptor.capture()))
                .thenReturn(mockedSearchContacts);
            when(mockedSearchContacts.setReadMask(stringArgumentCaptor.capture()))
                .thenReturn(mockedSearchContacts);
            when(mockedSearchContacts.setPageSize(integerArgumentCaptor.capture()))
                .thenReturn(mockedSearchContacts);
            when(mockedSearchContacts.execute())
                .thenReturn(mockedSearchResponse);
            when(mockedSearchResponse.getResults())
                .thenReturn(List.of(searchResult));

            List<Person> result = GoogleContactsSearchContactsAction
                .perform(mockedParameters, mockedParameters, mockedActionContext);

            assertNotNull(result);
            assertEquals(List.of(person), result);

            assertEquals(List.of("Name", "names,organizations"), stringArgumentCaptor.getAllValues());
            assertEquals(5, integerArgumentCaptor.getValue());
        }
    }
}
