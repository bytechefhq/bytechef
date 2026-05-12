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

package com.bytechef.component.assetfile;

import static com.bytechef.component.assetfile.constant.AssetFileConstants.ASSET_FILE;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.assetfile.action.AssetFileDeleteAction;
import com.bytechef.component.assetfile.action.AssetFileDownloadAction;
import com.bytechef.component.assetfile.action.AssetFileFindAction;
import com.bytechef.component.assetfile.action.AssetFileGetAction;
import com.bytechef.component.assetfile.action.AssetFileRenameAction;
import com.bytechef.component.assetfile.action.AssetFileUpdateContentAction;
import com.bytechef.component.assetfile.action.AssetFileUploadAction;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import org.springframework.stereotype.Component;

/**
 * Asset File component providing actions and AI agent tools for working with workspace asset file storage.
 *
 * @author Ivica Cardic
 */
@Component(ASSET_FILE + "_v1_ComponentHandler")
public class AssetFileComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public AssetFileComponentHandler(AssetFileFacade assetFileFacade) {
        this.componentDefinition = new AssetFileComponentDefinitionImpl(assetFileFacade);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AssetFileComponentDefinitionImpl extends AbstractComponentDefinitionWrapper {

        public AssetFileComponentDefinitionImpl(AssetFileFacade assetFileFacade) {
            super(buildDefinition(assetFileFacade));
        }

        private static ComponentDefinition buildDefinition(AssetFileFacade assetFileFacade) {
            ActionDefinition uploadAction = AssetFileUploadAction.of(assetFileFacade);
            ActionDefinition downloadAction = AssetFileDownloadAction.of(assetFileFacade);
            ActionDefinition getAction = AssetFileGetAction.of(assetFileFacade);
            ActionDefinition findAction = AssetFileFindAction.of(assetFileFacade);
            ActionDefinition updateContentAction = AssetFileUpdateContentAction.of(assetFileFacade);
            ActionDefinition renameAction = AssetFileRenameAction.of(assetFileFacade);
            ActionDefinition deleteAction = AssetFileDeleteAction.of(assetFileFacade);

            return component(ASSET_FILE)
                .title("Asset File")
                .description("Work with ByteChef workspace asset files: upload, download, list, rename and delete " +
                    "binary files stored in the workspace asset file infrastructure.")
                .icon("path:assets/asset-file.svg")
                .categories(ComponentCategory.HELPERS)
                .actions(
                    uploadAction, downloadAction, getAction, findAction, updateContentAction, renameAction,
                    deleteAction)
                .clusterElements(
                    tool(uploadAction), tool(downloadAction), tool(getAction), tool(findAction),
                    tool(updateContentAction), tool(renameAction), tool(deleteAction));
        }
    }
}
