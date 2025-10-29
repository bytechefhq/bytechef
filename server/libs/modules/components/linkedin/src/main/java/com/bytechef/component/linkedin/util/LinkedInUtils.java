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

import static com.bytechef.component.linkedin.constant.LinkedInConstants.DOCUMENT;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.IMAGE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.IMAGES;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInUtils {

    private LinkedInUtils() {
    }

    public static String uploadContent(Context context, FileEntry fileEntry, String personUrn, String contentType) {
        Map<String, Object> bodyMap = initializeUpload(context, contentType, personUrn);

        if (bodyMap.get("value") instanceof Map<?, ?> map) {
            String uploadUrl = (String) map.get("uploadUrl");

            context.http(http -> http.put(uploadUrl))
                .body(Http.Body.of(Map.of("file", fileEntry), Http.BodyContentType.FORM_DATA))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute();

            switch (contentType) {
                case DOCUMENT -> {
                    return (String) map.get(DOCUMENT);
                }
                case IMAGES -> {
                    return (String) map.get(IMAGE);
                }
                default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
            }
        }

        throw new ProviderException("Failed to upload image to LinkedIn.");
    }

    private static Map<String, Object> initializeUpload(Context context, String contentType, String personUrn) {
        String url = switch (contentType) {
            case IMAGES -> "/v2/images";
            case DOCUMENT -> "/rest/documents";
            default -> throw new ProviderException("Unsupported content type: " + contentType);
        };

        return context.http(http -> http.post(url))
            .queryParameter("action", "initializeUpload")
            .body(Http.Body.of("initializeUploadRequest", Map.of("owner", personUrn)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
