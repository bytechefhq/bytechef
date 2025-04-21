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

package com.bytechef.component.spotify;

import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.spotify.action.SpotifyCreatePlaylistAction;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class SpotifyComponentHandler extends AbstractSpotifyComponentHandler {

    @Override
    public List<? extends ModifiableActionDefinition> getCustomActions() {
        return List.of(SpotifyCreatePlaylistAction.ACTION_DEFINITION);
    }

    @Override
    public List<ClusterElementDefinition<?>> getCustomClusterElements() {
        return List.of(tool(SpotifyCreatePlaylistAction.ACTION_DEFINITION));
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/spotify.svg");
    }
}
