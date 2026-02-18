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

package com.bytechef.component.x.util;

import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.ID;
import static com.bytechef.component.x.constant.XConstants.MEDIA;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class XUtils {

    private XUtils() {
    }

    public static String getAuthenticatedUserId(Context context) {
        Map<String, Object> body = context.http(http -> http.get("/users/me"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof Map<?, ?> dataMap) {
            return (String) dataMap.get(ID);
        }

        throw new ProviderException("Failed to get user id for authenticated user.");
    }

    public static String getUserIdByUsername(Context context, String username) {
        Map<String, Object> body = context.http(http -> http.get("/users/by/username/" + username))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof Map<?, ?> dataMap) {
            return (String) dataMap.get(ID);
        }

        throw new ProviderException("Failed to get user id for username: " + username);
    }

    public static String uploadMedia(Context context, FileEntry fileEntry, String mediaCategory) {
        byte[] bytes = context.file(file -> file.readAllBytes(fileEntry));
        String base64EncodedMedia = context.encoder(encoder -> encoder.base64Encode(bytes));

        Map<String, ?> body = context.http(http -> http.post("/media/upload"))
            .body(Http.Body.of(Map.of(MEDIA, base64EncodedMedia, "media_category", mediaCategory)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(DATA) instanceof Map<?, ?> map) {
            return (String) map.get(ID);
        }

        throw new ProviderException("Failed to upload media to X.");
    }
}
