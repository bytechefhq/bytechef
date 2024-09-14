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

package com.bytechef.component.spotify;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.spotify.action.SpotifyAddItemsToPlaylistAction;
import com.bytechef.component.spotify.action.SpotifyStartResumePlaybackAction;
import com.bytechef.component.spotify.connection.SpotifyConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractSpotifyComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("spotify")
            .title("Spotify")
            .description(
                "Spotify is a popular music streaming service that offers a vast library of songs, podcasts, and playlists for users to enjoy."))
                    .actions(modifyActions(SpotifyStartResumePlaybackAction.ACTION_DEFINITION,
                        SpotifyAddItemsToPlaylistAction.ACTION_DEFINITION))
                    .connection(modifyConnection(SpotifyConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
