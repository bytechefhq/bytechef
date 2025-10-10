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

package com.bytechef.component.clickup.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Monika Kušter
 */
public class ClickupConstants {

    public static final String FOLDER_ID = "folderId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String SPACE_ID = "spaceId";
    public static final String WORKSPACE_ID = "workspaceId";

    public static final ModifiableStringProperty FOLDER_ID_PROPERTY = string(FOLDER_ID)
        .label("Folder ID")
        .options((OptionsFunction<String>) ClickupUtils::getFolderIdOptions)
        .optionsLookupDependsOn(SPACE_ID, WORKSPACE_ID)
        .required(false);

    public static final ModifiableStringProperty SPACE_ID_PROPERTY = string(SPACE_ID)
        .label("Space ID")
        .options((OptionsFunction<String>) ClickupUtils::getSpaceIdOptions)
        .optionsLookupDependsOn(WORKSPACE_ID)
        .required(true);

    public static final ModifiableStringProperty WORKSPACE_ID_PROPERTY = string(WORKSPACE_ID)
        .label("Workspace ID")
        .options((OptionsFunction<String>) ClickupUtils::getWorkspaceIdOptions)
        .required(true);

    private ClickupConstants() {
    }
}
