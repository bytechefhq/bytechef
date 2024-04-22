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

package com.bytechef.component.microsoft.share.point;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.MICROSOFT_SHARE_POINT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.share.point.action.MicrosoftSharePointCreateFolderAction;
import com.bytechef.component.microsoft.share.point.action.MicrosoftSharePointCreateListAction;
import com.bytechef.component.microsoft.share.point.action.MicrosoftSharePointCreateListItemAction;
import com.bytechef.component.microsoft.share.point.action.MicrosoftSharePointUploadFileAction;
import com.bytechef.component.microsoft.share.point.connection.MicrosoftSharePointConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftSharePointComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MICROSOFT_SHARE_POINT)
        .title("Microsoft SharePoint")
        .description(
            "Microsoft SharePoint is a web-based collaborative platform that integrates with Microsoft Office, " +
                "providing document management, intranet, and content management features for organizations.")
        .icon("path:assets/microsoft-share-point.svg")
        .connection(MicrosoftSharePointConnection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftSharePointCreateFolderAction.ACTION_DEFINITION,
            MicrosoftSharePointCreateListAction.ACTION_DEFINITION,
            MicrosoftSharePointCreateListItemAction.ACTION_DEFINITION,
            MicrosoftSharePointUploadFileAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
