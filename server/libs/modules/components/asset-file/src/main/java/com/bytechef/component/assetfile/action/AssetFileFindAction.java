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

import static com.bytechef.component.assetfile.constant.AssetFileConstants.ENVIRONMENT;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.TAG_IDS;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.WORKSPACE_ID;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.assetfile.util.AssetFileUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * Find Asset Files: List asset files in a workspace, optionally filtered by tags.
 *
 * @author Ivica Cardic
 */
public class AssetFileFindAction {

    private final AssetFileFacade assetFileFacade;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(AssetFileFacade assetFileFacade) {
        return new AssetFileFindAction(assetFileFacade).build();
    }

    private AssetFileFindAction(AssetFileFacade assetFileFacade) {
        this.assetFileFacade = assetFileFacade;
    }

    private ModifiableActionDefinition build() {
        return action("findAssetFiles")
            .title("Find Asset Files")
            .description("List asset files in a workspace. Optionally filter by tag ids.")
            .properties(
                integer(WORKSPACE_ID)
                    .label("Workspace ID")
                    .description("Workspace whose asset files to list.")
                    .required(true),
                integer(ENVIRONMENT)
                    .label("Environment")
                    .description(
                        "Environment ordinal (0 = DEVELOPMENT, 1 = STAGING, 2 = PRODUCTION) to filter by. Defaults to "
                            + "DEVELOPMENT when omitted.")
                    .defaultValue(0)
                    .required(false),
                array(TAG_IDS)
                    .label("Tag IDs")
                    .description("Optional list of tag ids to filter by; only files tagged with at least one of the " +
                        "given tags are returned.")
                    .items(integer()))
            .output(outputSchema(array().items((ValueProperty<?>) AssetFileUtils.assetFileItemSchema())))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        long workspaceId = inputParameters.getRequiredLong(WORKSPACE_ID);
        int environment = inputParameters.getInteger(ENVIRONMENT, 0);
        List<Long> tagIds = inputParameters.getList(TAG_IDS, Long.class, List.of());

        List<AssetFile> assetFiles = assetFileFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, environment,
            tagIds);

        return AssetFileUtils.toMaps(assetFiles);
    }
}
