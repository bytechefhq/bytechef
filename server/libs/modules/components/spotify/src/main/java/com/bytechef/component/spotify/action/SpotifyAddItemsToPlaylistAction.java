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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.spotify.util.SpotifyUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SpotifyAddItemsToPlaylistAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addItemsToPlaylist")
        .title("Add Items to a Playlist")
        .description("Adds one or more items to your playlist.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/playlists/{playlist_id}/tracks", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("playlist_id").label("Playlist ID")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) SpotifyUtils::getPlaylistIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            array("uris").items(string().description("URI's of the items to add to the playlist."))
                .placeholder("Add to Uris")
                .label("Tracks")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("position").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Position")
                .description("Position to insert the items, a zero-based index.")
                .required(false))
        .output(outputSchema(object().properties(string("snapshot_id").description("The snapshot ID of the playlist.")
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private SpotifyAddItemsToPlaylistAction() {
    }
}
