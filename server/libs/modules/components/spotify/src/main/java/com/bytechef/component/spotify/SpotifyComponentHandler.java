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
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.spotify.action.SpotifyCreatePlaylistAction;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
@AutoService(OpenApiComponentHandler.class)
public class SpotifyComponentHandler extends AbstractSpotifyComponentHandler {

    @Override
    public List<ModifiableActionDefinition> getCustomActions() {
        return List.of(SpotifyCreatePlaylistAction.ACTION_DEFINITION);
    }

    @Override
    public List<ModifiableClusterElementDefinition<?>> getCustomClusterElements() {
        return List.of(tool(SpotifyCreatePlaylistAction.ACTION_DEFINITION));
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            switch (name) {
                case "addItemsToPlaylist" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/spotify_v1#add-items-to-a-playlist");
                case "startResumePlayback" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/spotify_v1#playresume-playback");
                default -> {
                }
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .customActionHelp(
                "Spotify Web API documentation", "https://developer.spotify.com/documentation/web-api")
            .icon("path:assets/spotify.svg")
            .version(1);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .help("", "https://docs.bytechef.io/reference/components/spotify_v1#connection-setup")
            .version(1);
    }
}
