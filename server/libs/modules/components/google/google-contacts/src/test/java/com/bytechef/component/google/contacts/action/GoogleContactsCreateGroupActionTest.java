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

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.ContactGroup;
import com.google.api.services.people.v1.model.CreateContactGroupRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class GoogleContactsCreateGroupActionTest extends AbstractGoogleContactsActionTest {

    private final PeopleService.ContactGroups.Create mockedCreate =
        mock(PeopleService.ContactGroups.Create.class);
    private final ContactGroup mockedContactGroup = mock(ContactGroup.class);
    private final PeopleService.ContactGroups mockedContactGroups = mock(PeopleService.ContactGroups.class);
    private final ArgumentCaptor<CreateContactGroupRequest> createContactGroupRequestArgumentCaptor =
        ArgumentCaptor.forClass(CreateContactGroupRequest.class);

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getRequiredString(NAME))
            .thenReturn("Name");

        when(mockedPeopleService.contactGroups())
            .thenReturn(mockedContactGroups);
        when(mockedContactGroups.create(createContactGroupRequestArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedContactGroup);

        ContactGroup result = GoogleContactsCreateGroupAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedContactGroup, result);

        CreateContactGroupRequest createContactGroupRequest = createContactGroupRequestArgumentCaptor.getValue();

        assertEquals("Name", createContactGroupRequest.getContactGroup().getName());

    }
}
