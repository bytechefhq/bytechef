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
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class SendgridUtils {

    private SendgridUtils() {
    }

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

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
}
