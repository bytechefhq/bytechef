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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.spotify.constant.SpotifyConstants.ID;
import static com.bytechef.component.spotify.constant.SpotifyConstants.NAME;
import static com.bytechef.component.spotify.util.SpotifyUtils.getCurrentUserId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class SpotifyCreatePlaylistAction {

    protected static final String DESCRIPTION = "description";
    protected static final String PUBLIC = "public";
    protected static final String COLLABORATIVE = "collaborative";

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
                        bool(COLLABORATIVE),
                        string(DESCRIPTION),
                        object("external_urls")
                            .properties(
                                string("spotify")),
                        string("href"),
                        string(ID),
                        string(NAME),
                        string("type"),
                        string("uri"),
                        object("owner")
                            .properties(
                                string("href"),
                                string(ID),
                                string("type"),
                                string("uri")),
                        bool(PUBLIC))))
        .perform(SpotifyCreatePlaylistAction::perform);

    private SpotifyCreatePlaylistAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/users/" + getCurrentUserId(actionContext) + "/playlists"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    PUBLIC, inputParameters.getRequiredBoolean(PUBLIC),
                    COLLABORATIVE, inputParameters.getRequiredBoolean(COLLABORATIVE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
