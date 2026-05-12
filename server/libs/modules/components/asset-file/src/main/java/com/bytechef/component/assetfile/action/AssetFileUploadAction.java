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

import static com.bytechef.component.assetfile.constant.AssetFileConstants.CONTENT_TYPE;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.ENVIRONMENT;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.FILE;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.FILENAME;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.WORKSPACE_ID;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.assetfile.util.AssetFileUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.Map;

/**
 * Upload Asset File: Upload a workflow file entry into the workspace asset file storage.
 *
 * @author Ivica Cardic
 */
public class AssetFileUploadAction {

    private final AssetFileFacade assetFileFacade;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(AssetFileFacade assetFileFacade) {
        return new AssetFileUploadAction(assetFileFacade).build();
    }

    private AssetFileUploadAction(AssetFileFacade assetFileFacade) {
        this.assetFileFacade = assetFileFacade;
    }

    private ModifiableActionDefinition build() {
        return action("uploadAssetFile")
            .title("Upload Asset File")
            .description("Upload a workflow file entry into a workspace's asset file storage.")
            .properties(
                integer(WORKSPACE_ID)
                    .label("Workspace ID")
                    .description("Workspace into which the file should be uploaded.")
                    .required(true),
                integer(ENVIRONMENT)
                    .label("Environment")
                    .description(
                        "Environment ordinal (0 = DEVELOPMENT, 1 = STAGING, 2 = PRODUCTION) the uploaded file should "
                            + "belong to. Defaults to DEVELOPMENT when omitted, matching the legacy single-environment "
                            + "behaviour for workflows that have not been updated.")
                    .defaultValue(0)
                    .required(false),
                fileEntry(FILE)
                    .label("File")
                    .description("File entry to upload.")
                    .required(true),
                string(FILENAME)
                    .label("Filename")
                    .description("Optional filename override; defaults to the file entry's filename."),
                string(CONTENT_TYPE)
                    .label("Content Type")
                    .description("Optional content type override; defaults to the file entry's mime type."))
            .output(outputSchema(AssetFileUtils.assetFileSchema()))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        long workspaceId = inputParameters.getRequiredLong(WORKSPACE_ID);
        int environment = inputParameters.getInteger(ENVIRONMENT, 0);
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        String filename = inputParameters.getString(FILENAME, fileEntry.getName());
        String contentType = inputParameters.getString(CONTENT_TYPE, fileEntry.getMimeType());

        InputStream inputStream = actionContext.file(file -> file.getInputStream(fileEntry));

        AssetFile assetFile = assetFileFacade.createFromUpload(
            workspaceId, environment, filename, contentType, inputStream);

        return AssetFileUtils.toMap(assetFile);
    }
}
