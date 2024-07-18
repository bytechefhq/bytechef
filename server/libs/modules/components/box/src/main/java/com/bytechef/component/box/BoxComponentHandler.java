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

package com.bytechef.component.box;

import static com.bytechef.component.box.constant.BoxConstants.BOX;
import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.box.action.BoxCreateFolderAction;
import com.bytechef.component.box.action.BoxDownloadFileAction;
import com.bytechef.component.box.action.BoxUploadFileAction;
import com.bytechef.component.box.connection.BoxConnection;
import com.bytechef.component.box.trigger.BoxNewFileTrigger;
import com.bytechef.component.box.trigger.BoxNewFolderTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class BoxComponentHandler implements ComponentHandler {

    private static final ComponentDefinition componentDefinition = component(BOX)
        .title("Box")
        .description(
            "Box is a cloud content management and file sharing service that enables businesses to securely " +
                "store, manage, and collaborate on documents.")
        .icon("path:assets/box.svg")
        .categories(ComponentCategory.FILE_STORAGE)
        .connection(BoxConnection.CONNECTION_DEFINITION)
        .actions(
            BoxCreateFolderAction.ACTION_DEFINITION,
            BoxDownloadFileAction.ACTION_DEFINITION,
            BoxUploadFileAction.ACTION_DEFINITION)
        .triggers(
            BoxNewFileTrigger.TRIGGER_DEFINITION,
            BoxNewFolderTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
