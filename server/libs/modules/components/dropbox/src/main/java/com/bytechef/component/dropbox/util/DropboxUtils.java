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

package com.bytechef.component.dropbox.util;

import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FROM_PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MUTE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.STRICT_CONFLICT;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TO_PATH;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxUtils {

    private DropboxUtils() {
    }

    public static Object copy(Parameters inputParameters, Context context) {
        return context.http(http -> http.post("https://api.dropboxapi.com/2/files/copy_v2"))
            .body(
                Body.of(
                    FROM_PATH, inputParameters.getRequiredString(FROM_PATH),
                    TO_PATH, inputParameters.getRequiredString(TO_PATH),
                    AUTORENAME, inputParameters.getBoolean(AUTORENAME)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    public static Object delete(Parameters inputParameters, Context context) {
        return context.http(http -> http.post("https://api.dropboxapi.com/2/files/delete_v2"))
            .body(Body.of(Map.of(PATH, inputParameters.getRequiredString(PATH))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    public static Object move(Parameters inputParameters, Context context) {
        return context.http(http -> http.post("https://api.dropboxapi.com/2/files/move_v2"))
            .body(
                Body.of(
                    FROM_PATH, inputParameters.getRequiredString(FROM_PATH),
                    TO_PATH, inputParameters.getRequiredString(TO_PATH),
                    AUTORENAME, inputParameters.getBoolean(AUTORENAME)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    public static Object uploadFile(Parameters inputParameters, Context context, FileEntry fileEntry) {
        String headerJson = context.json(json -> {
            Map<String, Object> ime = Map.of(
                AUTORENAME, inputParameters.getBoolean(AUTORENAME),
                "mode", "add",
                MUTE, inputParameters.getBoolean(MUTE),
                PATH, getFullPath(inputParameters.getRequiredString(PATH), inputParameters.getRequiredString(FILENAME)),
                STRICT_CONFLICT, inputParameters.getBoolean(STRICT_CONFLICT));

            return json.write(ime);
        });

        return context.http(http -> http.post("https://content.dropboxapi.com/2/files/upload"))
            .headers(Map.of("Dropbox-API-Arg", List.of(headerJson)))
            .body(Body.of(fileEntry, "application/octet-stream"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static String getFullPath(String path, String filename) {
        return (path.endsWith("/") ? path : path + "/") + filename;
    }
}
