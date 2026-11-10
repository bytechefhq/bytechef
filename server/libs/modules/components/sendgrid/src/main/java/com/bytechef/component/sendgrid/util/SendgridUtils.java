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

package com.bytechef.component.sendgrid.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.FROM;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Marija Horvat
 */
public class SendgridUtils {

    private SendgridUtils() {
    }

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final String PAGE_TOKEN = "page_token";
    private static final String PAGE_SIZE = "1000";

    public static List<Map<String, Object>> getAllAttachments(List<FileEntry> attachmentFiles, Context context) {
        if (attachmentFiles == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> allAttachments = new ArrayList<>();

        for (FileEntry attachment : attachmentFiles) {
            String fileContent = context.file(file -> ENCODER.encodeToString(file.readAllBytes(attachment)));

            Map<String, Object> fileDetails = new HashMap<>();

            fileDetails.put("content", fileContent);
            fileDetails.put("filename", attachment.getName());
            fileDetails.put("type", attachment.getMimeType());

            allAttachments.add(fileDetails);
        }

        return allAttachments;
    }

    public static List<Map<String, String>> convertToEmailList(List<String> emailList) {
        return emailList.stream()
            .map(email -> Collections.singletonMap("email", email))
            .toList();
    }

    public static Object sendEmail(Context context, Map<String, Object> body) {
        context.http(http -> http.post("/mail/send"))
            .body(Body.of(body))
            .configuration(responseType(ResponseType.JSON))
            .execute();

        return null;
    }

    public static List<Option<String>> getTemplateIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(http -> http.get("/templates"))
            .queryParameter("generations", "dynamic")
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("templates") instanceof List<?> templates) {
            for (Object template : templates) {
                if (template instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Map<?, ?>> getContactLists(Context context) {
        List<Map<?, ?>> contactLists = new ArrayList<>();
        String pageToken = null;

        do {
            Executor executor = context.http(http -> http.get("/marketing/lists"))
                .queryParameter("page_size", PAGE_SIZE)
                .configuration(responseType(ResponseType.JSON));

            if (pageToken != null) {
                executor.queryParameter(PAGE_TOKEN, pageToken);
            }

            Map<String, Object> body = executor.execute()
                .getBody(new TypeReference<>() {});

            if (body.get("result") instanceof List<?> results) {
                for (Object result : results) {
                    if (result instanceof Map<?, ?> contactList) {
                        contactLists.add(contactList);
                    }
                }
            }

            String nextPageToken = getNextPageToken(body);

            if (Objects.equals(nextPageToken, pageToken)) {
                break;
            }

            pageToken = nextPageToken;
        } while (pageToken != null);

        return contactLists;
    }

    public static List<Map<?, ?>> getSenders(Context context) {
        Object body = context.http(http -> http.get("/marketing/senders"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();

        if (body instanceof Map<?, ?> bodyMap) {
            body = bodyMap.get("results");
        }

        List<Map<?, ?>> senders = new ArrayList<>();

        if (body instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> sender) {
                    senders.add(sender);
                }
            }
        }

        return senders;
    }

    public static List<Option<String>> getListIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        for (Map<?, ?> contactList : getContactLists(context)) {
            options.add(option((String) contactList.get(NAME), (String) contactList.get(ID)));
        }

        return options;
    }

    public static List<Option<Long>> getSenderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<Long>> options = new ArrayList<>();

        for (Map<?, ?> sender : getSenders(context)) {
            if (sender.get(ID) instanceof Number id) {
                String label = (String) sender.get("nickname");

                if (sender.get(FROM) instanceof Map<?, ?> from && from.get("email") instanceof String email) {
                    label = label + " (" + email + ")";
                }

                options.add(option(label, id.longValue()));
            }
        }

        return options;
    }

    public static List<Option<Long>> getSuppressionGroupIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> suppressionGroups = context.http(http -> http.get("/asm/groups"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, Object> suppressionGroup : suppressionGroups) {
            if (suppressionGroup.get(ID) instanceof Number id) {
                options.add(option((String) suppressionGroup.get(NAME), id.longValue()));
            }
        }

        return options;
    }

    private static String getNextPageToken(Map<String, Object> body) {
        if (body.get("_metadata") instanceof Map<?, ?> metadata && metadata.get("next") instanceof String nextUrl) {
            URI nextUri = URI.create(nextUrl);

            String query = nextUri.getRawQuery();

            if (query == null) {
                return null;
            }

            for (String queryParameter : query.split("&")) {
                if (queryParameter.startsWith(PAGE_TOKEN + "=")) {
                    return URLDecoder.decode(
                        queryParameter.substring(PAGE_TOKEN.length() + 1), StandardCharsets.UTF_8);
                }
            }
        }

        return null;
    }
}
