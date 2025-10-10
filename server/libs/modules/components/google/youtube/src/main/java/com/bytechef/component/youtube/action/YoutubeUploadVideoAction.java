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

package com.bytechef.component.youtube.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.youtube.constant.YoutubeConstants.CATEGORY_ID;
import static com.bytechef.component.youtube.constant.YoutubeConstants.DESCRIPTION;
import static com.bytechef.component.youtube.constant.YoutubeConstants.FILE;
import static com.bytechef.component.youtube.constant.YoutubeConstants.LOCATION;
import static com.bytechef.component.youtube.constant.YoutubeConstants.PRIVACY_STATUS;
import static com.bytechef.component.youtube.constant.YoutubeConstants.SNIPPET;
import static com.bytechef.component.youtube.constant.YoutubeConstants.STATUS;
import static com.bytechef.component.youtube.constant.YoutubeConstants.TAGS;
import static com.bytechef.component.youtube.constant.YoutubeConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.youtube.util.YoutubeUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class YoutubeUploadVideoAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadVideo")
        .title("Upload Video")
        .description("Uploads video to Youtube.")
        .properties(
            fileEntry(FILE)
                .label("Video File")
                .description("Video file that will be uploaded.")
                .required(true),
            string(TITLE)
                .label("Video Title")
                .description("Title of the video.")
                .required(true),
            string(DESCRIPTION)
                .label("Video Description")
                .description("Description of the video.")
                .required(true),
            array(TAGS)
                .label("Video Tags")
                .description("Tags of the video.")
                .items(
                    string("tag"))
                .required(false),
            string(PRIVACY_STATUS)
                .label("Privacy Status")
                .description("Privacy status of the video.")
                .required(true)
                .options(
                    option("Private", "private"),
                    option("Public", "public"),
                    option("Unlisted", "unlisted")),
            string(CATEGORY_ID)
                .label("Video Category ID")
                .options((OptionsFunction<String>) YoutubeUtils::getVideoCategoryIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("publishedAt")
                            .description("The date and time when the video was published."),
                        string("channelId")
                            .description("ID of the channel where the video was uploaded."),
                        string("title")
                            .description("Title of the video."),
                        string("description")
                            .description("Description of the video."),
                        object("thumbnails")
                            .description("Video thumbnails of different quality.")
                            .properties(
                                object("default")
                                    .description("Default quality thumbnail of the video.")
                                    .properties(
                                        string("url")
                                            .description("URL of the thumbnail."),
                                        integer("width")
                                            .description("Width of the thumbnail."),
                                        integer("height")
                                            .description("Height of the thumbnail.")),
                                object("medium")
                                    .description("Medium quality thumbnail of the video.")
                                    .properties(
                                        string("url")
                                            .description("URL of the thumbnail."),
                                        integer("width")
                                            .description("Width of the thumbnail."),
                                        integer("height")
                                            .description("Height of the thumbnail.")),
                                object("high")
                                    .description("High quality thumbnail of the video.")
                                    .properties(
                                        string("url")
                                            .description("URL of the thumbnail."),
                                        integer("width")
                                            .description("Width of the thumbnail."),
                                        integer("height")
                                            .description("Height of the thumbnail."))),
                        string("channelTitle")
                            .description("Title of the channel."),
                        array("tags")
                            .description("Tags of the video.")
                            .items(string()),
                        string("categoryId")
                            .description("ID of the video category."),
                        string("liveBroadcastContent")
                            .description("Live broadcasting content."),
                        object("localized")
                            .description("Localized description of the video.")
                            .properties(
                                string("title")
                                    .description("Title of the video."),
                                string("description")
                                    .description("Description of the video.")),
                        string("publishTime")
                            .description("The date and time when the video was published."))))
        .perform(YoutubeUploadVideoAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private YoutubeUploadVideoAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String url = context.http(http -> http.post("https://www.googleapis.com/upload/youtube/v3/videos"))
            .header("Content-Type", "application/octet-stream")
            .queryParameters("uploadType", "resumable", "part", "snippet,status")
            .body(
                Body.of(
                    SNIPPET, Map.of(
                        CATEGORY_ID, inputParameters.getRequiredString(CATEGORY_ID),
                        DESCRIPTION, inputParameters.getRequiredString(DESCRIPTION),
                        TITLE, inputParameters.getRequiredString(TITLE),
                        TAGS, inputParameters.getList(TAGS, List.of())),
                    STATUS, Map.of(PRIVACY_STATUS, inputParameters.getRequiredString(PRIVACY_STATUS))))
            .execute()
            .getHeaders()
            .get(LOCATION)
            .getFirst();

        Map<String, Object> response = context.http(http -> http.put(url))
            .header("Content-Type", "application/octet-stream")
            .body(Body.of(inputParameters.getRequiredFileEntry(FILE)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get(SNIPPET);
    }
}
