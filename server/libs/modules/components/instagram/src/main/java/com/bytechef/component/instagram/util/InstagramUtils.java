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

package com.bytechef.component.instagram.util;

import static com.bytechef.component.instagram.constant.InstagramConstants.COMMENT_FIELDS;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InstagramUtils {

    protected static final int CONTAINER_STATUS_MAX_ATTEMPTS = 10;
    protected static final long CONTAINER_STATUS_POLL_INTERVAL_MILLIS = 30_000;

    private InstagramUtils() {
    }

    public static String createMediaContainer(Context context, String userId, Map<String, Object> body) {
        Map<String, Object> response = context.http(http -> http.post("/" + userId + "/media"))
            .body(Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getId(response, "Instagram did not return a media container ID");
    }

    public static List<Map<String, Object>> getComments(Context context, String mediaId) {
        Map<String, Object> response = context.http(http -> http.get("/" + mediaId + "/comments"))
            .queryParameter("fields", COMMENT_FIELDS)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getData(response);
    }

    public static List<Map<String, Object>> getData(Map<String, Object> response) {
        List<Map<String, Object>> items = new ArrayList<>();

        if (response != null && response.get("data") instanceof List<?> data) {
            for (Object item : data) {
                if (item instanceof Map<?, ?> itemMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedItemMap = (Map<String, Object>) itemMap;

                    items.add(typedItemMap);
                }
            }
        }

        return items;
    }

    public static String getUserId(Context context) {
        Map<String, Object> response = context.http(http -> http.get("/me"))
            .queryParameter("fields", "user_id")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response == null || response.get("user_id") == null) {
            throw new ProviderException("Instagram did not return the user ID of the connected account");
        }

        return String.valueOf(response.get("user_id"));
    }

    public static Map<String, Object> publishMediaContainer(Context context, String userId, String creationId) {
        Map<String, Object> response = context.http(http -> http.post("/" + userId + "/media_publish"))
            .body(Body.of(Map.of("creation_id", creationId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String mediaId = getId(response, "Instagram did not return the ID of the published media");

        return Map.of("id", mediaId);
    }

    public static void waitUntilContainerFinished(Context context, String containerId) {
        for (int attempt = 1; attempt <= CONTAINER_STATUS_MAX_ATTEMPTS; attempt++) {
            Map<String, Object> response = context.http(http -> http.get("/" + containerId))
                .queryParameter("fields", "status_code,status")
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            String statusCode = response == null ? null : (String) response.get("status_code");

            if ("FINISHED".equals(statusCode)) {
                return;
            }

            if ("ERROR".equals(statusCode) || "EXPIRED".equals(statusCode)) {
                throw new ProviderException(
                    "Instagram could not process media container " + containerId + ": " + statusCode + " " +
                        response.get("status"));
            }

            if (attempt < CONTAINER_STATUS_MAX_ATTEMPTS) {
                sleep(containerId);
            }
        }

        throw new ProviderException(
            "Instagram did not finish processing media container " + containerId + " after " +
                CONTAINER_STATUS_MAX_ATTEMPTS + " status checks");
    }

    private static String getId(Map<String, Object> response, String errorMessage) {
        if (response == null || response.get("id") == null) {
            throw new ProviderException(errorMessage);
        }

        return String.valueOf(response.get("id"));
    }

    private static void sleep(String containerId) {
        try {
            Thread.sleep(CONTAINER_STATUS_POLL_INTERVAL_MILLIS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new ProviderException(
                "Interrupted while waiting for media container " + containerId, interruptedException);
        }
    }
}
