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

package com.bytechef.component.google.drive;

import static com.bytechef.component.google.drive.connection.GoogleDriveConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_DRIVE;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.google.drive.action.GoogleDriveUploadAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 */
@AutoService(ComponentHandler.class)
public class GoogleDriveComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(GOOGLE_DRIVE)
        .title("Google Drive")
        .description("Component description.")
        .icon("path:assets/google-drive.svg")
        .connection(CONNECTION_DEFINITION)
        .actions(GoogleDriveUploadAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
