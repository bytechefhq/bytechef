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

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.youtube.constant.YoutubeConstants.FILE;


public class YoutubeTesting {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("test")
        .title("Test")
        .description("Uploads video to Youtube.")
        .properties()
        .output(outputSchema(string()))
        .perform(YoutubeTesting::perform);

    private YoutubeTesting() {
    }

    public static Map<String, Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

//        return context.http(http -> http.get("https://www.googleapis.com/youtube/v3/playlists?part=snippet" +
//                "&channelId=UC_x5XG1OV2P6uZZ5FSM9Ttw" +
//                "&maxResults=25"
        Map<String, Object> result = context.http(http -> http.get("https://www.googleapis.com/youtube/v3/playlists"))
            .queryParameters("part", "snippet"
                , "mine", "true")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return result;
    }
}

//byte[] binaryFile =  context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));
//
//ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
//        byteArrayOutputStream.write(binaryFile);
//
//String url = context.http(http -> http.post("https://www.googleapis.com/upload/youtube/v3/videos"))
//    .headers(Map.of("Content-Type", List.of("application/octet-stream")
//    ))
//    .queryParameters("part", "snippet,status", "uploadType", "resumable")
//    .body(
//        Body.of(
//            "snippet", Map.of(
//                "categoryId", inputParameters.getRequiredString("videoCategoryId"),
//                "description", "This is the description.",
//                "title", "This is the Title",
//                "tags", List.of("youtube", "test")),
//            "status", Map.of("privacyStatus", "private")
//        )
//    )
//
//    .execute()
//    .getHeaders()
//    .get("location")
//    .getFirst();
//
//Object result = context.http(http -> http.put(url))
//    .headers(Map.of("Content-Type", List.of("application/octet-stream")))
//    .body(
//        Body.of(binaryFile)
//    )
//    .configuration(responseType(Http.ResponseType.JSON))
//    .execute()
//    .getBody(new TypeReference<>() {});
//
//
//        return null;
