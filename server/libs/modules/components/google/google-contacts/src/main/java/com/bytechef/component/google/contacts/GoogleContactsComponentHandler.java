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

package com.bytechef.component.google.contacts;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.google.contacts.connection.GoogleContactsConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.contacts.action.GoogleContactsCreateContactAction;
import com.bytechef.component.google.contacts.action.GoogleContactsCreateGroupAction;
import com.bytechef.component.google.contacts.action.GoogleContactsSearchContactsAction;
import com.bytechef.component.google.contacts.action.GoogleContactsUpdateContactAction;
import com.bytechef.component.google.contacts.cluster.GoogleContactsCreateContactTool;
import com.bytechef.component.google.contacts.cluster.GoogleContactsCreateGroupTool;
import com.bytechef.component.google.contacts.cluster.GoogleContactsSearchContactsTool;
import com.bytechef.component.google.contacts.cluster.GoogleContactsUpdateContactTool;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class GoogleContactsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleContacts")
        .title("Google Contacts")
        .description(
            "Google Contacts is a cloud-based address book service provided by Google, allowing users to store, " +
                "manage, and synchronize their contact information across multiple devices and platforms.")
        .customAction(true)
        .icon("path:assets/google-contacts.svg")
        .categories(ComponentCategory.CRM)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleContactsCreateContactAction.ACTION_DEFINITION,
            GoogleContactsCreateGroupAction.ACTION_DEFINITION,
            GoogleContactsUpdateContactAction.ACTION_DEFINITION,
            GoogleContactsSearchContactsAction.ACTION_DEFINITION)
        .clusterElements(
            GoogleContactsCreateContactTool.CLUSTER_ELEMENT_DEFINITION,
            GoogleContactsCreateGroupTool.CLUSTER_ELEMENT_DEFINITION,
            GoogleContactsUpdateContactTool.CLUSTER_ELEMENT_DEFINITION,
            GoogleContactsSearchContactsTool.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
