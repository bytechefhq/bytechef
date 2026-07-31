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
import static com.bytechef.component.assetfile.constant.AssetFileConstants.CONTENT_TYPE;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.FILE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.assetfile.util.AssetFileContextResolver;
import com.bytechef.component.assetfile.util.AssetFileUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.Map;

/**
 * Update Asset File Content: Replace the binary content of an existing asset file. The file must belong to the
 * workspace owning the executing workflow.
 *
 * @author Ivica Cardic
 */
public class AssetFileUpdateContentAction {

    private final AssetFileFacade assetFileFacade;
    private final AssetFileContextResolver contextResolver;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        AssetFileFacade assetFileFacade, AssetFileContextResolver contextResolver) {

        return new AssetFileUpdateContentAction(assetFileFacade, contextResolver).build();
    }

    private AssetFileUpdateContentAction(
        AssetFileFacade assetFileFacade, AssetFileContextResolver contextResolver) {

        this.assetFileFacade = assetFileFacade;
        this.contextResolver = contextResolver;
    }

    private ModifiableActionDefinition build() {
        return action("updateAssetFileContent")
            .title("Update Asset File Content")
            .description("Replace the binary content of an existing asset file.")
            .properties(
                integer(ASSET_FILE_ID)
                    .label("Asset File ID")
                    .description("Identifier of the asset file whose content to replace.")
                    .required(true),
                fileEntry(FILE)
                    .label("File")
                    .description("File entry whose content replaces the existing asset file content.")
                    .required(true),
                string(CONTENT_TYPE)
                    .label("Content Type")
                    .description("Optional content type override; defaults to the file entry's mime type."))
            .output(outputSchema(AssetFileUtils.assetFileSchema()))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        long workspaceId = contextResolver.resolveWorkspaceId(actionContext);
        long assetFileId = inputParameters.getRequiredLong(ASSET_FILE_ID);

        assetFileFacade.findByIdInWorkspace(assetFileId, workspaceId);

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        String contentType = inputParameters.getString(CONTENT_TYPE, fileEntry.getMimeType());

        InputStream inputStream = actionContext.file(file -> file.getInputStream(fileEntry));

        AssetFile assetFile = assetFileFacade.updateContent(assetFileId, contentType, inputStream);

        return AssetFileUtils.toMap(assetFile);
    }
}
