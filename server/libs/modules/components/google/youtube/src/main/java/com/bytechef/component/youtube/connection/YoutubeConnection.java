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

package com.bytechef.component.youtube.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.google.commons.GoogleConnection;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class YoutubeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = GoogleConnection.createConnection(
        "https://www.googleapis.com/youtube/v3",
        1,
        "",
        (connection, context) -> Map.of(
            "https://www.googleapis.com/auth/youtube", true,
            "https://www.googleapis.com/auth/youtube.channel-memberships.creator", false,
            "https://www.googleapis.com/auth/youtube.force-ssl", false,
            "https://www.googleapis.com/auth/youtube.readonly", false,
            "https://www.googleapis.com/auth/youtube.upload", true,
            "https://www.googleapis.com/auth/youtubepartner", false,
            "https://www.googleapis.com/auth/youtubepartner-channel-audit", false));

    private YoutubeConnection() {
    }
}
