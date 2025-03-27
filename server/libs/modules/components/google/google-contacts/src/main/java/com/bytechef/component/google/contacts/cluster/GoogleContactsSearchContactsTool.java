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

package com.bytechef.component.google.contacts.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.google.contacts.action.GoogleContactsCreateContactAction.OUTPUT_SCHEMA;
import static com.bytechef.component.google.contacts.action.GoogleContactsCreateContactAction.PROPERTIES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.SEARCH_CONTACTS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.SEARCH_CONTACTS_DESCRIPTION;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.SEARCH_CONTACTS_TITLE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.google.contacts.action.GoogleContactsCreateContactAction;

/**
 * @author Monika Kušter
 */
public class GoogleContactsSearchContactsTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(SEARCH_CONTACTS)
            .title(SEARCH_CONTACTS_TITLE)
            .description(SEARCH_CONTACTS_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .output(OUTPUT_SCHEMA)
            .object(() -> GoogleContactsCreateContactAction::perform);
}
