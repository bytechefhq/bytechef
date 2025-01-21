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

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleContactsCreateContactActionTest extends AbstractGoogleContactsActionTest {

    private final PeopleService.People.CreateContact mockedCreateContact =
        mock(PeopleService.People.CreateContact.class);
    private final PeopleService.People mockedPeople = mock(PeopleService.People.class);
    private final Person mockedPerson = mock(Person.class);
    private final ArgumentCaptor<Person> personArgumentCaptor = ArgumentCaptor.forClass(Person.class);

    @Test
    void testPerform() throws IOException {
        mockedParameters = MockParametersFactory.create(
            Map.of(
                GIVEN_NAME, "First name", MIDDLE_NAME, "Middle name", FAMILY_NAME, "Last name",
                EMAIL, "mail@mail.com", PHONE_NUMBER, "123456", NAME, "Company", TITLE, "Job title"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getPeopleService(mockedParameters))
                .thenReturn(mockedPeopleService);

            when(mockedPeopleService.people())
                .thenReturn(mockedPeople);
            when(mockedPeople.createContact(personArgumentCaptor.capture()))
                .thenReturn(mockedCreateContact);
            when(mockedCreateContact.execute())
                .thenReturn(mockedPerson);

            Person result =
                GoogleContactsCreateContactAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(mockedPerson, result);

            Person person = personArgumentCaptor.getValue();

            List<Name> names = person.getNames();

            assertEquals(1, names.size());

            Name name = names.getFirst();

            assertEquals("First name", name.getGivenName());
            assertEquals("Middle name", name.getMiddleName());
            assertEquals("Last name", name.getFamilyName());

            List<EmailAddress> emailAddresses = person.getEmailAddresses();

            assertEquals(1, emailAddresses.size());

            EmailAddress emailAddress = emailAddresses.getFirst();

            assertEquals("mail@mail.com", emailAddress.getValue());

            List<PhoneNumber> phoneNumbers = person.getPhoneNumbers();

            assertEquals(1, emailAddresses.size());

            PhoneNumber phoneNumber = phoneNumbers.getFirst();

            assertEquals("123456", phoneNumber.getValue());

            List<Organization> organizations = person.getOrganizations();

            assertEquals(1, organizations.size());

            Organization organization = organizations.getFirst();

            assertEquals("Company", organization.getName());
            assertEquals("Job title", organization.getTitle());
        }
    }
}
