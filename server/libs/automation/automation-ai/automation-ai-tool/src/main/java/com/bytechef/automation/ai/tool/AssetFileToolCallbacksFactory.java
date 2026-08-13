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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the asset-file tool-callback lists shared by the Copilot panel agents, the AI Hub {@code asset_file_agent}
 * subagent, and the management MCP server. Read list feeds ASK; write list feeds BUILD.
 *
 * @author Ivica Cardic
 */
public class AssetFileToolCallbacksFactory {

    private final AssetFileFacade assetFileFacade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AssetFileToolCallbacksFactory(
        AssetFileFacade assetFileFacade, @Nullable ToolArtifactRecorder artifactRecorder) {

        this.assetFileFacade = assetFileFacade;
        this.artifactRecorder = artifactRecorder;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListAssetFilesToolCallback(assetFileFacade));
        toolCallbacks.add(new GetAssetFileContentToolCallback(assetFileFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateAssetFileToolCallback(assetFileFacade, artifactRecorder));
        toolCallbacks.add(new CreateBinaryAssetFileToolCallback(assetFileFacade, artifactRecorder));
        toolCallbacks.add(new UpdateAssetFileContentToolCallback(assetFileFacade, artifactRecorder));
        toolCallbacks.add(new CloneAssetFileToolCallback(assetFileFacade));
        toolCallbacks.add(new CreateAssetFileFromUrlToolCallback(assetFileFacade, artifactRecorder));

        return toolCallbacks;
    }
}
