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

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.COMPANY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FIRST_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.JOB_TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.LAST_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Martin Tarasovič
 */
public class GoogleContactsUpdateContactActionTest extends AbstractGoogleContactsActionTest {

    private final PeopleService.People.UpdateContact mockedUpdateContact =
        mock(PeopleService.People.UpdateContact.class);
    private final PeopleService.People.Get mockedGetContact =
        mock(PeopleService.People.Get.class);
    private final PeopleService.People mockedPeople = mock(PeopleService.People.class);
    private final Person mockedPerson = mock(Person.class);
    private final ArgumentCaptor<Person> personArgumentCaptor = ArgumentCaptor.forClass(Person.class);
    private final ArgumentCaptor<String> resourceNameArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        Name originalName = new Name()
            .setGivenName("Original first name")
            .setMiddleName("Original middle name")
            .setFamilyName("Original last name");
        EmailAddress originalAddress = new EmailAddress().setValue("original@mail.com");
        PhoneNumber originalPhoneNumber = new PhoneNumber().setValue("123456789");
        Organization originalOrganization = new Organization()
            .setTitle("Original job title")
            .setName("Original company");

        Person originalPerson = new Person()
            .setResourceName("people/c1234567890123456789")
            .setNames(List.of(originalName))
            .setEmailAddresses(List.of(originalAddress))
            .setPhoneNumbers(List.of(originalPhoneNumber))
            .setOrganizations(List.of(originalOrganization));

        when(mockedParameters.getRequiredString(RESOURCE_NAME))
            .thenReturn("people/c1234567890123456789");
        when(mockedParameters.getRequiredString(FIRST_NAME))
            .thenReturn("First name");
        when(mockedParameters.getString(MIDDLE_NAME))
            .thenReturn("Middle name");
        when(mockedParameters.getRequiredString(LAST_NAME))
            .thenReturn("Last name");
        when(mockedParameters.getString(EMAIL))
            .thenReturn("mail@mail.com");
        when(mockedParameters.getString(PHONE_NUMBER))
            .thenReturn("123456");
        when(mockedParameters.getString(COMPANY))
            .thenReturn("Company");
        when(mockedParameters.getString(JOB_TITLE))
            .thenReturn("Job title");

        when(mockedPeopleService.people())
            .thenReturn(mockedPeople);
        when(mockedPeople.get(resourceNameArgumentCaptor.capture()))
            .thenReturn(mockedGetContact);
        when(mockedGetContact.setPersonFields(anyString()))
            .thenReturn(mockedGetContact);
        when(mockedGetContact.execute())
            .thenReturn(originalPerson);
        when(mockedPeople.updateContact(resourceNameArgumentCaptor.capture(), personArgumentCaptor.capture()))
            .thenReturn(mockedUpdateContact);
        when(mockedUpdateContact.setUpdatePersonFields(anyString()))
            .thenReturn(mockedUpdateContact);
        when(mockedUpdateContact.execute())
            .thenReturn(mockedPerson);

        Person result = GoogleContactsUpdateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedPerson, result);
        assertNotEquals(originalPerson, result);

        Person person = personArgumentCaptor.getValue();

        assertEquals(originalPerson.getResourceName(), person.getResourceName());

        List<Name> names = person.getNames();

        assertEquals(1, names.size());

        Name name = names.getFirst();

        assertNotEquals(originalName.getGivenName(), name.getGivenName());
        assertEquals("First name", name.getGivenName());
        assertNotEquals(originalName.getMiddleName(), name.getMiddleName());
        assertEquals("Middle name", name.getMiddleName());
        assertNotEquals(originalName.getFamilyName(), name.getFamilyName());
        assertEquals("Last name", name.getFamilyName());

        List<EmailAddress> emailAddresses = person.getEmailAddresses();

        assertEquals(1, emailAddresses.size());

        EmailAddress emailAddress = emailAddresses.getFirst();

        assertNotEquals(originalAddress.getValue(), emailAddress.getValue());
        assertEquals("mail@mail.com", emailAddress.getValue());

        List<PhoneNumber> phoneNumbers = person.getPhoneNumbers();

        assertEquals(1, phoneNumbers.size());

        PhoneNumber phoneNumber = phoneNumbers.getFirst();

        assertNotEquals(originalPhoneNumber.getValue(), phoneNumber.getValue());
        assertEquals("123456", phoneNumber.getValue());

        List<Organization> organizations = person.getOrganizations();

        assertEquals(1, organizations.size());

        Organization organization = organizations.getFirst();

        assertNotEquals(originalOrganization.getName(), organization.getName());
        assertEquals("Company", organization.getName());
        assertNotEquals(originalOrganization.getTitle(), organization.getTitle());
        assertEquals("Job title", organization.getTitle());
    }

}
