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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
                .required(true))
        .output(outputSchema(string()))
        .perform(YoutubeUploadVideoAction::perform);

    private YoutubeUploadVideoAction() {
    }

    public static Map<String, Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String boundary = "boundary_string";
        byte[] binaryFile =   context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));

        return context.http(http -> http.post("https://www.googleapis.com/upload/youtube/v3/videos"))
//            .header("X-Upload-Content-Type", "video/mp4")
//        "Content-Length", String.valueOf(binaryFile.length)
            .queryParameters("part", "snippet,status", "uploadType", "resumable")
            .body(
                Body.of(
                    "snippet", Map.of("title","Test Upload")))
//            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
