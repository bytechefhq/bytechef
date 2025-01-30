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

package com.bytechef.component.clickup.constant;

import static com.bytechef.component.definition.ComponentDsl.number;

import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;

/**
 * @author Monika Kušter
 */
public class ClickupConstants {

    public static final String FOLDER_ID = "folderId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String SPACE_ID = "spaceId";
    public static final String WORKSPACE_ID = "workspaceId";

    public static final ModifiableNumberProperty FOLDER_ID_PROPERTY = number(FOLDER_ID)
        .label("Folder ID")
        .options((ActionOptionsFunction<String>) ClickupUtils::getFolderIdOptions)
        .optionsLookupDependsOn(SPACE_ID, WORKSPACE_ID)
        .required(false);

    public static final ModifiableNumberProperty SPACE_ID_PROPERTY = number(SPACE_ID)
        .label("Space ID")
        .options((ActionOptionsFunction<String>) ClickupUtils::getSpaceIdOptions)
        .optionsLookupDependsOn(WORKSPACE_ID)
        .required(true);

    public static final ModifiableNumberProperty WORKSPACE_ID_PROPERTY = number(WORKSPACE_ID)
        .label("Workspace ID")
        .options((ActionOptionsFunction<String>) ClickupUtils::getWorkspaceIdOptions)
        .required(true);

    private ClickupConstants() {
    }
}
