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

package com.bytechef.component.youtube.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.youtube.constant.YoutubeConstants.FILE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.youtube.util.YoutubeUtils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

//https://www.googleapis.com/auth/youtube.upload
//https://www.googleapis.com/auth/youtube

public class YoutubeUploadVideoAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadVideo")
        .title("Upload Video")
        .description("Uploads video to Youtube.")
        .properties(
            fileEntry(FILE)
                .label("Video File")
                .required(true),
            string("videoCategoryId")
                    .label("Video Category ID")
                    .options((ActionOptionsFunction<String>) YoutubeUtils::getVideoCategoryIdOptions)
                    .required(true))
        .output(outputSchema(string()))
        .perform(YoutubeUploadVideoAction::perform);

    private YoutubeUploadVideoAction() {
    }

    public static Map<String, Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) throws IOException {
        byte[] binaryFile =   context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));
        String url = context.http(http -> http.post("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"))
    .headers(Map.of("Content-Type", List.of("application/octet-stream")
    ))
//    .queryParameters("part", "snippet,status", "uploadType", "resumable")
    .body(
        Body.of(
            "snippet", Map.of(
                "categoryId", inputParameters.getRequiredString("videoCategoryId"),
                "description", "This is the description.",
                "title", "This is the Title",
                "tags", List.of("youtube", "test")),
            "status", Map.of("privacyStatus", "private")
        )
    ).execute()
    .getHeaders()
    .get("location")
    .getFirst();

//        String jsonRequest = context.json(json -> json.write(Map.of(
//            "snippet", Map.of(
//                "categoryId", "22",
//                "description", "This is the descriptdzion.",
//                "title", "This is the TitlSCe",
//                "tags", List.of()),
//            "status", Map.of("privacyStatus", "private")
//        )));
//
//        Response response = context.http(http -> http.post("https://www.googleapis.com/upload/youtube/v3/videos"))
//                .headers(Map.of(
//                    "Content-Type", List.of("application/json; charset=UTF-8"),
//                                "X-Upload-Content-Type", List.of("application/octet-stream")
////                                "X-Upload-Content-Type", List.of("video/*")
////                    , "X-Upload-Content-Length",  List.of(String.valueOf(binaryFile.length))
//                    ))
//            .queryParameters(
//                "uploadType", "resumable",
//                "part", "snippet,status")
//                .body(
//                        Body.of(
////                            jsonRequest
////                            Map.of(
//                            "snippet", Map.of(
//                                "categoryId", "22",
//                                "description", "This is the descriptdzion.",
//                                "title", "This is the TitlSCe",
//                                "tags", List.of()),
//                            "status", Map.of("privacyStatus", "private")
////                            )
//
//                        )
//                )
//            .execute();

//            String url = response.getHeaders()
//            .get("location")
//            .getFirst();

        Object result = context.http(http -> http.put(url))
    .headers(Map.of("Content-Type", List.of("application/octet-stream")))
    .body(
        Body.of(binaryFile)
    )
    .configuration(responseType(Http.ResponseType.JSON))
    .execute()
    .getBody(new TypeReference<>() {});

        return null;
    }
}
