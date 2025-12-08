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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.ALBUM_ID;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.FILE_BINARY;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.FILE_NAME;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.MEDIA;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.MEDIA_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.photos.util.GooglePhotosUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GooglePhotosUploadMediaAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadMedia")
        .title("Upload Media")
        .description("Upload media items.")
        .properties(
            array(MEDIA)
                .label("Media")
                .description("Media files to upload.")
                .items(
                    object()
                        .properties(
                            fileEntry(FILE_BINARY)
                                .label("File Binary Data")
                                .description("File binary data to upload.")
                                .required(true),
                            string(FILE_NAME)
                                .label("File Name")
                                .description(
                                    "File name with extension of the media item shown to the user in Google Photos.")
                                .required(false))),
            string(ALBUM_ID)
                .label("Album ID")
                .description("Identifier of the album where the media items are added.")
                .options((ActionDefinition.OptionsFunction<String>) GooglePhotosUtils::getAlbumsOptions)
                .required(true))
        .output(outputSchema(MEDIA_OUTPUT_PROPERTY))
        .perform(GooglePhotosUploadMediaAction::perform);

    private GooglePhotosUploadMediaAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> mediaItems = new ArrayList<>();

        List<Media> items = inputParameters.getRequiredList(MEDIA, Media.class);

        for (Media item : items) {
            getUploadToken(context, item, mediaItems);
        }

        Map<String, Object> body = Map.of(
            ALBUM_ID, inputParameters.getRequiredString(ALBUM_ID),
            "newMediaItems", mediaItems);

        return context.http(http -> http.post("/mediaItems:batchCreate"))
            .configuration(responseType(ResponseType.JSON))
            .body(Body.of(body))
            .execute()
            .getBody();
    }

    private static void getUploadToken(Context context, Media item, List<Map<String, Object>> mediaItems) {
        FileEntry fileEntry = item.fileBinary();
        String filename = item.fileName();

        String uploadToken = context.http(http -> http.post("/uploads"))
            .headers(
                Map.of(
                    "Content-type", List.of("application/octet-stream"),
                    "X-Goog-Upload-Content-Type", List.of(fileEntry.getMimeType()),
                    "X-Goog-Upload-Protocol", List.of("raw")))
            .body(Body.of(fileEntry))
            .configuration(responseType(ResponseType.TEXT))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> mediaItem = Map.of(
            "simpleMediaItem", Map.of(
                "uploadToken", uploadToken,
                "fileName", filename != null ? filename : fileEntry.getName()));

        mediaItems.add(mediaItem);
    }

    public record Media(String fileName, FileEntry fileBinary) {
    }
}
