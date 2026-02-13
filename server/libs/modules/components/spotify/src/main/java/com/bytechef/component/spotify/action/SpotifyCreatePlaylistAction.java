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

package com.bytechef.component.spotify.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.spotify.constant.SpotifyConstants.COLLABORATIVE;
import static com.bytechef.component.spotify.constant.SpotifyConstants.DESCRIPTION;
import static com.bytechef.component.spotify.constant.SpotifyConstants.ID;
import static com.bytechef.component.spotify.constant.SpotifyConstants.NAME;
import static com.bytechef.component.spotify.constant.SpotifyConstants.PUBLIC;
import static com.bytechef.component.spotify.util.SpotifyUtils.getCurrentUserId;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class SpotifyCreatePlaylistAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPlaylist")
        .title("Create Playlist")
        .description("Creates a new playlist")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name for the new playlist.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("The description for the new playlist.")
                .required(false),
            bool(PUBLIC)
                .label("Public")
                .description("The public status for the new playlist.")
                .defaultValue(true)
                .required(true),
            bool(COLLABORATIVE)
                .label("Collaborative")
                .description("If the playlist is collaborative or not.")
                .defaultValue(false)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool(COLLABORATIVE)
                            .description("Indicates if the owner allows other users to modify the playlist."),
                        string(DESCRIPTION)
                            .description("The playlist description."),
                        object("external_urls")
                            .description("Known external URLs for this playlist.")
                            .properties(
                                string("spotify")
                                    .description("The Spotify URL for the playlist.")),
                        string("href")
                            .description("A link to the Web API endpoint providing full details of the playlist."),
                        string(ID)
                            .description("The Spotify ID for the playlist."),
                        string(NAME)
                            .description("The name of the playlist."),
                        string("type")
                            .description("The object type: 'playlist'."),
                        string("uri")
                            .description("The Spotify URI for the playlist."),
                        object("owner")
                            .description("The user who owns the playlist.")
                            .properties(
                                string("href")
                                    .description("A link to the Web API endpoint providing full details of the user."),
                                string(ID)
                                    .description("The Spotify ID for the user."),
                                string("type")
                                    .description("The object type: 'user'."),
                                string("uri")
                                    .description("The Spotify URI for the user.")),
                        bool(PUBLIC)
                            .description("The playlist's public/private status."))))
        .help("", "https://docs.bytechef.io/reference/components/spotify_v1#create-playlist")
        .perform(SpotifyCreatePlaylistAction::perform);

    private SpotifyCreatePlaylistAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/users/" + getCurrentUserId(context) + "/playlists"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    PUBLIC, inputParameters.getRequiredBoolean(PUBLIC),
                    COLLABORATIVE, inputParameters.getRequiredBoolean(COLLABORATIVE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
