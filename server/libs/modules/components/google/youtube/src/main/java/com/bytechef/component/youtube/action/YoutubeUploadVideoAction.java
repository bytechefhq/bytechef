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
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.youtube.util.YoutubeUtils;

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
                .required(true),
            string("videoCategoryId")
                    .label("Video Category ID")
                    .options((ActionOptionsFunction<String>) YoutubeUtils::getVideoCategoryIdOptions)
                    .required(true))
        .output(outputSchema(string()))
        .perform(YoutubeUploadVideoAction::perform);

    private YoutubeUploadVideoAction() {
    }

    public static Map<String, Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String boundary = "boundary_string";
        byte[] binaryFile =   context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));
        StringBuilder binaryVideoString = "";

        for (byte b : binaryFile) {
            // Convert byte to 8-bit binary string
            String bin = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binaryVideoString.append(bin);
        }

        return context.http(http -> http.post("https://www.googleapis.com/upload/youtube/v3/videos"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .queryParameters("part", "snippet,status")
                .body(
                        Body.of(
                                "--boundary_string\n" +
                                        "Content-Type: application/json; charset=UTF-8\n" +
                                        "\n" +
                                        "{\n" +
                                        "  \"snippet\": {\n" +
                                        "    \"title\": \"Test Upload via Bytechef\",\n" +
                                        "    \"description\": \"This is a test video upload using Postman and the YouTube API\",\n" +
                                        "    \"tags\": [\"test\", \"postman\", \"youtube\"],\n" +
                                        "    \"categoryId\": \"22\"\n" +
                                        "  },\n" +
                                        "  \"status\": {\n" +
                                        "    \"privacyStatus\": \"private\"\n" +
                                        "  }\n" +
                                        "}\n" +
                                        "\n" +
                                        "--boundary_string\n" +
                                        "Content-Type: video/*\n" +
                                        "\n" +
                                        binaryFile.toString() +
                                        "\n" +
                                        "--boundary_string--"
                        )
                )
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
