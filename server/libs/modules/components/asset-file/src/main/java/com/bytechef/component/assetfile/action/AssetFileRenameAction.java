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

package com.bytechef.component.assetfile.action;

import static com.bytechef.component.assetfile.constant.AssetFileConstants.ASSET_FILE_ID;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.NEW_NAME;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.assetfile.util.AssetFileUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * Rename Asset File: Rename an asset file.
 *
 * @author Ivica Cardic
 */
public class AssetFileRenameAction {

    private final AssetFileFacade assetFileFacade;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(AssetFileFacade assetFileFacade) {
        return new AssetFileRenameAction(assetFileFacade).build();
    }

    private AssetFileRenameAction(AssetFileFacade assetFileFacade) {
        this.assetFileFacade = assetFileFacade;
    }

    private ModifiableActionDefinition build() {
        return action("renameAssetFile")
            .title("Rename Asset File")
            .description("Rename an asset file.")
            .properties(
                integer(ASSET_FILE_ID)
                    .label("Asset File ID")
                    .description("Identifier of the asset file to rename.")
                    .required(true),
                string(NEW_NAME)
                    .label("New Name")
                    .description("New name for the asset file.")
                    .required(true))
            .output(outputSchema(AssetFileUtils.assetFileSchema()))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        long assetFileId = inputParameters.getRequiredLong(ASSET_FILE_ID);
        String newName = inputParameters.getRequiredString(NEW_NAME);

        AssetFile assetFile = assetFileFacade.rename(assetFileId, newName);

        return AssetFileUtils.toMap(assetFile);
    }
}
