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

package com.bytechef.component.google.forms.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.ANSWERS;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FILE_UPLOAD_ANSWERS;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONDENT_EMAIL;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSES;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.TEXT_ANSWERS;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

    public static Map<String, Object> createCustomResponse(Context context, String formId, Map<?, ?> response) {
        List<FormItem> formItems = getFormItems(getForm(formId, context));

        Map<String, Object> responses = new LinkedHashMap<>();

        responses.put(FORM_ID, formId);
        responses.put(RESPONSE_ID, response.get(RESPONSE_ID));
        responses.put(RESPONDENT_EMAIL, response.get(RESPONDENT_EMAIL));

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

    public static List<Map<String, Object>> getCustomResponses(Context context, String formId, String timestamp) {
        List<Map<?, ?>> formResponses = getFormResponses(formId, context, timestamp);

        List<Map<String, Object>> customResponses = new ArrayList<>();

        for (Map<?, ?> formResponse : formResponses) {
            customResponses.add(createCustomResponse(context, formId, formResponse));
        }
        return customResponses;
    }

    public static Map<String, Object> getForm(String formId, Context context) {
        return context
            .http(http -> http.get("/forms/" + formId))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static List<Option<String>> getResponseIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> formResponses = new ArrayList<>();
        String nextToken = null;
        String formId = inputParameters.getRequiredString(FORM_ID);
        do {
            Map<String, Object> response = context
                .http(http -> http.get("/forms/%s/responses".formatted(formId)))
                .queryParameters(NEXT_PAGE_TOKEN, nextToken)
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
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

    private static List<Map<?, ?>> getFormResponses(String formId, Context context, String timestamp) {
        List<Map<?, ?>> formResponses = new ArrayList<>();
        String nextToken = null;

        do {
            List<Object> queryParameters = new ArrayList<>();

            queryParameters.add("pageSize");
            queryParameters.add(5000);

            if (nextToken != null) {
                queryParameters.add(NEXT_PAGE_TOKEN);
                queryParameters.add(nextToken);
            }

            if (timestamp != null) {
                queryParameters.add("filter");
                queryParameters.add("timestamp > " + timestamp);
            }

            Map<String, Object> response = context
                .http(http -> http.get("/forms/%s/responses".formatted(formId)))
                .queryParameters(queryParameters.toArray())
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            extractResponses(response, formResponses);
            nextToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);
        } while (nextToken != null);

        formResponses.sort(Comparator.comparing(map -> Instant.parse((String) map.get("createTime"))));

        return formResponses.reversed();
    }

    private static void extractResponses(Map<String, Object> response, List<Map<?, ?>> formResponses) {
        if (response.get(RESPONSES) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    formResponses.add(map);
                }
            }
        }
    }

    private static List<FormItem> getFormItems(Map<String, Object> body) {
        List<FormItem> formItems = new ArrayList<>();

        if (body.get("items") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map &&
                    map.get("questionItem") instanceof Map<?, ?> questionItem &&
                    questionItem.get("question") instanceof Map<?, ?> question) {

                    formItems.add(new FormItem((String) question.get("questionId"), (String) map.get("title")));
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
                        "question_" + index,
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
                "question_" + index,
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
