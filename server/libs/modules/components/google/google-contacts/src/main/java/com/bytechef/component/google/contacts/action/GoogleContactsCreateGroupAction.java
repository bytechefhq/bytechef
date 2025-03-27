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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CREATE_GROUP;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CREATE_GROUP_DESCRIPTION;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CREATE_GROUP_TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.ContactGroup;
import com.google.api.services.people.v1.model.CreateContactGroupRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 * @author Monika Domiter
 */
public class GoogleContactsCreateGroupAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(NAME)
            .label("Group Name")
            .description("The name of the group.")
            .required(true)
    };

    public static final OutputSchema<ObjectProperty> OUTPUT_SCHEMA = outputSchema(
        object()
            .properties(
                string(NAME)
                    .description(
                        "The contact group name set by the group owner or a system provided name for system groups.")));

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_GROUP)
        .title(CREATE_GROUP_TITLE)
        .description(CREATE_GROUP_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleContactsCreateGroupAction::perform);

    private GoogleContactsCreateGroupAction() {
    }

    public static ContactGroup perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

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
