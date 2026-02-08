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

package com.bytechef.component.google.workspace.admin;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.google.workspace.admin.connection.GoogleWorkspaceAdminConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.workspace.admin.action.GoogleWorkspaceAdminAssignLicenseAction;
import com.bytechef.component.google.workspace.admin.action.GoogleWorkspaceAdminAssignRoleToUserAction;
import com.bytechef.component.google.workspace.admin.action.GoogleWorkspaceAdminCreateUserAction;
import com.bytechef.component.google.workspace.admin.action.GoogleWorkspaceAdminSuspendUserAction;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class GoogleWorkspaceAdminComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleWorkspaceAdmin")
        .title("Google Workspace Admin")
        .description(
            "Google Workspace Admin is responsible for managing users, groups, devices, and security settings across " +
                "the organization.")
        .customAction(true)
        .icon("path:assets/google-workspace-admin.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleWorkspaceAdminCreateUserAction.ACTION_DEFINITION,
            GoogleWorkspaceAdminAssignRoleToUserAction.ACTION_DEFINITION,
            GoogleWorkspaceAdminAssignLicenseAction.ACTION_DEFINITION,
            GoogleWorkspaceAdminSuspendUserAction.ACTION_DEFINITION)
        .clusterElements(
            tool(GoogleWorkspaceAdminCreateUserAction.ACTION_DEFINITION),
            tool(GoogleWorkspaceAdminAssignRoleToUserAction.ACTION_DEFINITION),
            tool(GoogleWorkspaceAdminAssignLicenseAction.ACTION_DEFINITION),
            tool(GoogleWorkspaceAdminSuspendUserAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
