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

package com.bytechef.component.google.forms.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Vihar Shah
 */
public class GoogleFormsUtils {

    private GoogleFormsUtils() {
    }

    public static List<Option<String>> getFormOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        return drive.files()
            .list()
            .setQ("mimeType = 'application/vnd.google-apps.form' and trashed = false")
            .execute()
            .getFiles()
            .stream()
            .map(file -> (Option<String>) option(file.getName(), file.getId()))
            .toList();
    }

    public static List<Option<String>> getResponseOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> formResponses = new ArrayList<>();
        String nextToken = null;
        do {
            Http.Executor executor = context
                .http(http -> http.get(
                    "https://forms.googleapis.com/v1/forms/" + inputParameters.getRequiredString(FORM) + "/responses"))
                .configuration(Http.responseType(Http.ResponseType.JSON));

            if (nextToken != null) {
                executor.queryParameter("nextPageToken", nextToken);
            }

            Map<String, Object> response = executor.execute()
                .getBody(new TypeReference<>() {});

            nextToken = (String) response.getOrDefault("nextPageToken", null);

            if (response.get("responses") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String responseId = (String) map.get("responseId");
                        String respondentEmail = (String) map.get("respondentEmail");

                        formResponses.add(
                            option(
                                respondentEmail == null ? responseId : respondentEmail + " (" + responseId + ")",
                                responseId));
                    }
                }
            }

        } while (nextToken != null);

        return formResponses;
    }
}
