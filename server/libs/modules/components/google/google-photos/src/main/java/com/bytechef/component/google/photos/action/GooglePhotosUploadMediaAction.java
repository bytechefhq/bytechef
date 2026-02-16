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
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.MEDIA;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.MEDIA_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.photos.util.GooglePhotosUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marija Horvat
 */
public class GooglePhotosUploadMediaAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadMedia")
        .title("Upload Media")
        .description("Upload media to an album in a user's Google Photos library.")
        .properties(
            string(ALBUM_ID)
                .label("Album ID")
                .description("Identifier of the album where the media items are added.")
                .options((OptionsFunction<String>) GooglePhotosUtils::getAlbumIdOptions)
                .required(true),
            array(MEDIA)
                .label("Media")
                .description("Media files to upload to album. Photos and videos are supported.")
                .items(
                    object()
                        .properties(
                            fileEntry("fileEntry")
                                .label("Photo/Video")
                                .description("File entry of the media item to upload.")
                                .required(true),
                            string("fileName")
                                .label("File Name")
                                .description(
                                    "File name with extension of the media item to upload. If not specified, " +
                                        "the file name is taken from the file entry.")
                                .required(false)))
                .minItems(1)
                .required(true))
        .output(outputSchema(MEDIA_OUTPUT_PROPERTY))
        .perform(GooglePhotosUploadMediaAction::perform)
        .help("", "https://docs.bytechef.io/reference/components/google-photos_v1#upload-media");

    private GooglePhotosUploadMediaAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Media> items = inputParameters.getRequiredList(MEDIA, Media.class);

        List<Map<String, Object>> mediaItems = items.stream()
            .map(item -> buildMediaItem(context, item))
            .collect(Collectors.toList());

        return context.http(http -> http.post("/mediaItems:batchCreate"))
            .configuration(responseType(ResponseType.JSON))
            .body(
                Body.of(
                    ALBUM_ID, inputParameters.getRequiredString(ALBUM_ID),
                    "newMediaItems", mediaItems))
            .execute()
            .getBody();
    }

    private static Map<String, Object> buildMediaItem(Context context, Media media) {
        FileEntry fileEntry = media.fileEntry();

        String filename = media.fileName() == null ? fileEntry.getName() : media.fileName();
        String uploadToken = uploadFileAndGetToken(context, fileEntry);

        return Map.of("simpleMediaItem", Map.of("uploadToken", uploadToken, "fileName", filename));
    }

    private static String uploadFileAndGetToken(Context context, FileEntry fileEntry) {
        return context.http(http -> http.post("/uploads"))
            .headers(
                Map.of(
                    "Content-Type", List.of("application/octet-stream"),
                    "X-Goog-Upload-Content-Type", List.of(fileEntry.getMimeType()),
                    "X-Goog-Upload-Protocol", List.of("raw")))
            .body(Body.of(fileEntry))
            .configuration(responseType(ResponseType.TEXT))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public record Media(String fileName, FileEntry fileEntry) {
    }
}
