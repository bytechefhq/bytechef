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

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.forms.constant.GoogleFormsConstants;
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
        String searchText, Context context) throws IOException {

        String formId = inputParameters.getRequiredString(GoogleFormsConstants.FORM);

        List<Option<String>> formResponses = new ArrayList<>();
        Map<String, Object> response;
        String nextToken = null;
        do {
            Context.Http.Executor executor = context
                .http(http -> http.get("https://forms.googleapis.com/v1/forms/" + formId + "/responses"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON));

            if (nextToken != null) {
                executor.queryParameter("nextPageToken", nextToken);
            }
            response = executor.execute()
                .getBody(new TypeReference<>() {});

            nextToken = (String) response.getOrDefault("nextPageToken", null);
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("responses");
            items.stream()
                .map(item -> {
                    String respondentEmail = (String) item.get("respondentEmail");
                    String responseId = (String) item.get("responseId");
                    return (Option<String>) option(respondentEmail + "(" + responseId + ")", responseId);
                })
                .forEach(formResponses::add);
        } while (nextToken != null);
        return formResponses;
    }
}
