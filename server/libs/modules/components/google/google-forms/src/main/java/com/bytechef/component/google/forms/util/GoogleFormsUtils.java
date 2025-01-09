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
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.ANSWERS;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FILE_UPLOAD_ANSWERS;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSES;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.TEXT_ANSWERS;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Vihar Shah
 */
public class GoogleFormsUtils {

    private GoogleFormsUtils() {
    }

    public static Map<String, Object> getForm(String formId, Context context) {
        return context
            .http(http -> http.get("https://forms.googleapis.com/v1/forms/" + formId))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static List<Option<String>> getFormIdOptions(
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

    public static List<Option<String>> getResponseIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> formResponses = new ArrayList<>();
        String nextToken = null;
        String formId = inputParameters.getRequiredString(FORM_ID);
        do {
            Http.Executor executor = context
                .http(http -> http.get("https://forms.googleapis.com/v1/forms/%s/responses".formatted(formId)))
                .configuration(Http.responseType(Http.ResponseType.JSON));

            if (nextToken != null) {
                executor.queryParameter(NEXT_PAGE_TOKEN, nextToken);
            }

            Map<String, Object> response = executor.execute()
                .getBody(new TypeReference<>() {});

            nextToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);

            if (response.get(RESPONSES) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String responseId = (String) map.get(RESPONSE_ID);
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

    public static List<Map<?, ?>> getFormResponses(String formId, Context context, String startDateString) {
        String encode = URLEncoder.encode("timestamp > " + startDateString, StandardCharsets.UTF_8);
        List<Map<?, ?>> formResponses = new ArrayList<>();
        String nextToken = null;

        do {
            Http.Executor executor = context
                .http(http -> http.get("https://forms.googleapis.com/v1/forms/%s/responses".formatted(formId)))
                .queryParameter("filter", encode)
                .configuration(Http.responseType(Http.ResponseType.JSON));

            if (nextToken != null) {
                executor.queryParameter(NEXT_PAGE_TOKEN, nextToken);
            }

            Map<String, Object> response = executor.execute()
                .getBody(new TypeReference<>() {});

            nextToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);

            if (response.get(RESPONSES) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        formResponses.add(map);
                    }
                }
            }

        } while (nextToken != null);

        return formResponses;
    }

    public static Map<String, Object> getCustomResponse(Context context, String formId, Map<?, ?> response) {
        List<FormItem> formItems = getFormItems(getForm(formId, context));

        Map<String, Object> responses = new LinkedHashMap<>();

        responses.put(FORM_ID, formId);
        responses.put(RESPONSE_ID, response.get(RESPONSE_ID));

        if (response.get(ANSWERS) instanceof Map<?, ?> answers) {
            int index = 1;
            for (FormItem formItem : formItems) {
                String id = formItem.itemId();

                if (answers.get(id) instanceof Map<?, ?> answer) {
                    processAnswers(responses, formItem, answer, index++);
                }
            }
        }

        return responses;
    }

    private static List<FormItem> getFormItems(Map<String, Object> body) {
        List<FormItem> formItems = new ArrayList<>();

        if (body.get("items") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    if (map.get("questionItem") instanceof Map<?, ?> questionItem &&
                        questionItem.get("question") instanceof Map<?, ?> question) {

                        formItems.add(new FormItem((String) question.get("questionId"), (String) map.get("title")));
                    }
                }
            }
        }

        return formItems;
    }

    private static void processAnswers(Map<String, Object> responses, FormItem formItem, Map<?, ?> answer, int index) {
        if (answer.containsKey(FILE_UPLOAD_ANSWERS)) {
            processFileUploadAnswers(responses, formItem, answer, index);
        } else if (answer.containsKey(TEXT_ANSWERS)) {
            processTextAnswers(responses, formItem, answer, index);
        }
    }

    private static void processFileUploadAnswers(
        Map<String, Object> responses, FormItem formItem, Map<?, ?> answer, int index) {

        if (answer.get(FILE_UPLOAD_ANSWERS) instanceof Map<?, ?> textAnswers &&
            textAnswers.get(ANSWERS) instanceof List<?> list) {
            for (Object fileUploadAnswer : list) {
                if (fileUploadAnswer instanceof Map<?, ?> map) {
                    responses.put(
                        "question" + index,
                        new FormFileUploadAnswer(
                            formItem.itemId(), formItem.title(), (String) map.get("fileId"),
                            (String) map.get("fileName")));
                }
            }
        }
    }

    private static void processTextAnswers(
        Map<String, Object> responses, FormItem formItem, Map<?, ?> answer, int index) {

        if (answer.get(TEXT_ANSWERS) instanceof Map<?, ?> textAnswers &&
            textAnswers.get(ANSWERS) instanceof List<?> list) {
            List<String> answers = new ArrayList<>();
            for (Object textAnswer : list) {
                if (textAnswer instanceof Map<?, ?> map) {
                    answers.add((String) map.get("value"));
                }
            }
            responses.put(
                "question" + index,
                new FormTextAnswer(formItem.itemId(), formItem.title(), answers));
        }

    }

    record FormItem(String itemId, String title) {
    }

    record FormTextAnswer(String questionId, String title, List<String> answers) {
    }

    record FormFileUploadAnswer(String questionId, String title, String fileId, String fileName) {
    }
}
