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

package com.bytechef.component.linkedin.util;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInUtils {

    private LinkedInUtils() {
    }

    public static Map<String, Object> uploadImage(Context context, FileEntry image, String personUrn) {
        Map<String, Object> body = context.http(http -> http.post("/v2/images"))
            .queryParameter("action", "initializeUpload")
            .body(Http.Body.of("initializeUploadRequest", Map.of("owner", personUrn)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("value") instanceof Map<?, ?> map) {
            String uploadUrl = (String) map.get("uploadUrl");

            context.http(http -> http.put(uploadUrl))
                .header("Content-Type", "multipart/form-data")
                .body(Http.Body.of(Map.of("file", image), Http.BodyContentType.FORM_DATA))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute();
        }

        return body;
    }
}
