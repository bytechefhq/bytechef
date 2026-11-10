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

package com.bytechef.component.okta;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.okta.action.OktaGetUserAction;
import com.bytechef.component.okta.action.OktaListApplicationUsersAction;
import com.bytechef.component.okta.action.OktaListGroupAppsAction;
import com.bytechef.component.okta.action.OktaListGroupMembersAction;
import com.bytechef.component.okta.action.OktaListUserAppLinksAction;
import com.bytechef.component.okta.action.OktaListUserGroupsAction;
import com.bytechef.component.okta.action.OktaListUsersAction;
import com.bytechef.component.okta.connection.OktaConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class OktaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("okta")
        .title("Okta")
        .description(
            "Okta is an identity and access management platform that manages users, groups and application " +
                "assignments, providing single sign-on and lifecycle management for workforce and customer " +
                "identities.")
        .icon("path:assets/okta.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(OktaConnection.CONNECTION_DEFINITION)
        .actions(
            OktaGetUserAction.ACTION_DEFINITION,
            OktaListUsersAction.ACTION_DEFINITION,
            OktaListUserGroupsAction.ACTION_DEFINITION,
            OktaListUserAppLinksAction.ACTION_DEFINITION,
            OktaListGroupMembersAction.ACTION_DEFINITION,
            OktaListGroupAppsAction.ACTION_DEFINITION,
            OktaListApplicationUsersAction.ACTION_DEFINITION)
        .version(1)
        .customAction(true)
        .customActionHelp("", "https://developer.okta.com/docs/api/");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
