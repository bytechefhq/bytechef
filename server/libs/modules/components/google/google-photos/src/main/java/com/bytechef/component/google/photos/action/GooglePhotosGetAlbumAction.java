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
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.ALBUM_ID;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.ALBUM_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.photos.util.GooglePhotosUtils;

/**
 * @author Marija Horvat
 */
public class GooglePhotosGetAlbumAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getAlbum")
        .title("Get Album")
        .description("Returns the app created album based on the specified albumId.")
        .properties(
            string(ALBUM_ID)
                .label("Album ID")
                .description("Identifier of the album to be requested.")
                .options((OptionsFunction<String>) GooglePhotosUtils::getAlbumIdOptions)
                .required(true))
        .output(outputSchema(ALBUM_OUTPUT_PROPERTY))
        .perform(GooglePhotosGetAlbumAction::perform)
        .help("", "https://docs.bytechef.io/reference/components/google-photos_v1#get-album");

    private GooglePhotosGetAlbumAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/albums/" + inputParameters.getRequiredString(ALBUM_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
