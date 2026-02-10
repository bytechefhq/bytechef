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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.spotify.util.SpotifyUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SpotifyStartResumePlaybackAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("startResumePlayback")
        .title("Play/Resume Playback")
        .description("Start or resume current playback on an active device.")
        .metadata(
            Map.of(
                "method", "PUT",
                "path", "/me/player/play", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("deviceId").label("Device ID")
            .description(
                "The id of the device this command is targeting. If not supplied, the user's currently active device is the target.")
            .required(false)
            .options((ActionDefinition.OptionsFunction<String>) SpotifyUtils::getDeviceIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("context_uri").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Context Uri")
                .description("Spotify URI of the context to play (album, artist, playlist).")
                .required(false),
            array("uris").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("Spotify track URIs to play."))
                .placeholder("Add to Uris")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Tracks")
                .description("Spotify track URIs to play.")
                .required(false),
            integer("position_ms").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Position")
                .description("The position in milliseconds to start playback from.")
                .required(false));

    private SpotifyStartResumePlaybackAction() {
    }
}
