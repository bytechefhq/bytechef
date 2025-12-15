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

package com.bytechef.component.google.photos.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.ALBUM_OUTPUT_PROPERTY;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.TITLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GooglePhotosCreateAlbumAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createAlbum")
        .title("Create Album")
        .description("Creates an album in a user's Google Photos library.")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Name of the album.")
                .required(true)
                .maxLength(500))
        .output(outputSchema(ALBUM_OUTPUT_PROPERTY))
        .perform(GooglePhotosCreateAlbumAction::perform);

    private GooglePhotosCreateAlbumAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/albums"))
            .body(Body.of("album", Map.of(TITLE, inputParameters.getRequiredString(TITLE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
