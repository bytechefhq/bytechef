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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CREATE_GROUP;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.ContactGroup;
import com.google.api.services.people.v1.model.CreateContactGroupRequest;
import java.io.IOException;

/**
 * @author Monika Domiter
 */
public class GoogleContactsCreateGroupAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_GROUP)
        .title("Create groups")
        .description("Creates a new group")
        .properties(
            string(NAME)
                .label("Group name")
                .description("The name of the group")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(NAME)))
        .perform(GoogleContactsCreateGroupAction::perform);

    private GoogleContactsCreateGroupAction() {
    }

    public static ContactGroup perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        PeopleService peopleService = GoogleServices.getPeopleService(connectionParameters);

        CreateContactGroupRequest createContactGroupRequest = new CreateContactGroupRequest()
            .setContactGroup(
                new ContactGroup()
                    .setName(inputParameters.getRequiredString(NAME)));

        return peopleService
            .contactGroups()
            .create(createContactGroupRequest)
            .execute();
    }

}
