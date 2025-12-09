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

package com.bytechef.component.google.photos.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class GooglePhotosConstants {

    public static final String ALBUM_ID = "albumId";
    public static final String FILE_BINARY = "fileBinary";
    public static final String MEDIA = "media";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String TITLE = "title";

    public static final ModifiableObjectProperty ALBUM_OUTPUT_PROPERTY = object()
        .properties(
            string("id")
                .description("Identifier for the album."),
            string(TITLE)
                .description("Name of the album."),
            string("productUrl")
                .description("Google Photos URL for the album."),
            bool("isWriteable")
                .description("True if you can create media items in this album."),
            object("shareInfo")
                .properties(
                    object("sharedAlbumOptions")
                        .properties(
                            bool("isCollaborative")
                                .description(
                                    "True if the shared album allows collaborators to add media items to it."),
                            bool("isCommentable")
                                .description(
                                    "True if the shared album allows collaborators to add comments to the album.")),
                    string("shareableUrl")
                        .description("A link to the shared Google Photos album."),
                    string("shareToken")
                        .description(
                            "A token that is used to join, leave, or retrieve the details of a shared album on behalf of a user who isn't the owner."),
                    bool("isJoined")
                        .description("True if the user is joined to the album."),
                    bool("isOwned")
                        .description("True if the user owns the album."),
                    bool("isJoinable")
                        .description("True if the album can be joined by users.")),
            string("mediaItemsCount")
                .description("The number of media items in the album."),
            string("coverPhotoBaseUrl")
                .description("A URL to the cover photo's bytes."),
            string("coverPhotoMediaItemId")
                .description("Identifier for the media item associated with the cover photo."));

    public static final ModifiableObjectProperty MEDIA_OUTPUT_PROPERTY = object()
        .properties(
            array("newMediaItemResults")
                .items(
                    object()
                        .properties(
                            string("uploadToken")
                                .description("The upload token used to create this new media item."),
                            object("status")
                                .properties(
                                    integer("code")
                                        .description(
                                            "The status code, which should be an enum value of google.rpc.Code."),
                                    string("message")
                                        .description("A developer-facing error message."),
                                    array("details")
                                        .items(object())),
                            object("mediaItem")
                                .properties(
                                    string("id")
                                        .description("Identifier for the media item."),
                                    string("description")
                                        .description("Description of the media item."),
                                    string("productUrl")
                                        .description("Google Photos URL for the media item."),
                                    string("baseUrl")
                                        .description("A URL to the media item's bytes."),
                                    string("mimeType")
                                        .description("MIME type of the media item."),
                                    object("mediaMetadata")
                                        .properties(
                                            string("creationTime")
                                                .description("Time when the media item was first created."),
                                            string("width")
                                                .description("Original width (in pixels) of the media item."),
                                            string("height")
                                                .description("Original height (in pixels) of the media item."),
                                            object("photo")
                                                .properties(
                                                    string("cameraMake")
                                                        .description(
                                                            "Brand of the camera with which the photo was taken."),
                                                    string("cameraModel")
                                                        .description(
                                                            "Model of the camera with which the photo was taken."),
                                                    number("focalLength")
                                                        .description(
                                                            "Focal length of the camera lens with which the photo was taken."),
                                                    number("apertureFNumber")
                                                        .description(
                                                            "Aperture f number of the camera lens with which the photo was taken."),
                                                    integer("isoEquivalent")
                                                        .description(
                                                            "ISO of the camera with which the photo was taken."),
                                                    string("exposureTime")
                                                        .description(
                                                            "Exposure time of the camera aperture when the photo was taken.")),
                                            object("video")
                                                .properties(
                                                    string("cameraMake")
                                                        .description(
                                                            "Brand of the camera with which the video was taken."),
                                                    string("cameraModel")
                                                        .description(
                                                            "Model of the camera with which the video was taken."),
                                                    number("fps")
                                                        .description("Frame rate of the video."),
                                                    string("status")
                                                        .description("Processing status of the video."))),
                                    object("contributorInfo")
                                        .properties(
                                            string("profilePictureBaseUrl")
                                                .description("URL to the profile picture of the contributor."),
                                            string("displayName")
                                                .description("Display name of the contributor.")),
                                    string("filename")
                                        .description("Filename of the media item.")))));

    private GooglePhotosConstants() {
    }
}
