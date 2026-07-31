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

import static com.bytechef.component.assetfile.constant.AssetFileConstants.TAG_IDS;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.component.assetfile.util.AssetFileContextResolver;
import com.bytechef.component.assetfile.util.AssetFileUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * Find Asset Files: List the asset files of the workspace owning the executing workflow, optionally filtered by tags.
 *
 * @author Ivica Cardic
 */
public class AssetFileFindAction {

    private final AssetFileFacade assetFileFacade;
    private final AssetFileContextResolver contextResolver;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        AssetFileFacade assetFileFacade, AssetFileContextResolver contextResolver) {

        return new AssetFileFindAction(assetFileFacade, contextResolver).build();
    }

    private AssetFileFindAction(AssetFileFacade assetFileFacade, AssetFileContextResolver contextResolver) {
        this.assetFileFacade = assetFileFacade;
        this.contextResolver = contextResolver;
    }

    private ModifiableActionDefinition build() {
        return action("findAssetFiles")
            .title("Find Asset Files")
            .description(
                "List the asset files of the workspace owning this workflow, in the environment the workflow runs " +
                    "in. Optionally filter by tag ids.")
            .properties(
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

        long workspaceId = contextResolver.resolveWorkspaceId(actionContext);
        int environment = contextResolver.resolveEnvironment(actionContext);

        List<Long> tagIds = inputParameters.getList(TAG_IDS, Long.class, List.of());

        List<AssetFile> assetFiles = assetFileFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, environment,
            tagIds);

        return AssetFileUtils.toMaps(assetFiles);
    }
}
