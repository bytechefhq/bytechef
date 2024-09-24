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

package com.bytechef.component.dropbox.util;

import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MUTE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.STRICT_CONFLICT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxUtils {

    protected static final ContextFunction<Http, Http.Executor> POST_FILES_UPLOAD_CONTEXT_FUNCTION =
        http -> http.post("https://content.dropboxapi.com/2/files/upload");

    public static String getFullPath(String path, String filename) {
        return (path.endsWith("/") ? path : path + "/") + filename;
    }

    private DropboxUtils() {
    }

    public static Object uploadFile(Parameters inputParameters, ActionContext actionContext, FileEntry fileEntry) {
        String headerJson = actionContext.json(json -> {
            Map<String, Object> ime = Map.of(
                AUTORENAME, inputParameters.getBoolean(AUTORENAME),
                "mode", "add",
                MUTE, inputParameters.getBoolean(MUTE),
                PATH, getFullPath(inputParameters.getRequiredString(PATH), inputParameters.getRequiredString(FILENAME)),
                STRICT_CONFLICT, inputParameters.getBoolean(STRICT_CONFLICT));

            return json.write(ime);
        });

        return actionContext.http(POST_FILES_UPLOAD_CONTEXT_FUNCTION)
            .headers(Map.of("Dropbox-API-Arg", List.of(headerJson)))
            .body(Http.Body.of(fileEntry, "application/octet-stream"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
