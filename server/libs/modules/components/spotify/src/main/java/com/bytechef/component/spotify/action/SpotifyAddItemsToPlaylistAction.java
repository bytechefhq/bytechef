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

package com.bytechef.component.spotify.action;

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SpotifyAddItemsToPlaylistAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addItemsToPlaylist")
        .title("Add Items to a Playlist")
        .description("Adds one or more items to your playlist.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/playlists/{playlist_id}/tracks", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("playlist_id").label("Playlist")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            array("uris").items(string().description("URI's of the items to add to the playlist."))
                .placeholder("Add to Uris")
                .label("Uris")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("__item").properties(integer("position").label("Position")
                .description("Position to insert the items, a zero-based index.")
                .required(false))
                .label("Item")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object().properties(object("body").properties(string("snapshot_id").required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private SpotifyAddItemsToPlaylistAction() {
    }
}
