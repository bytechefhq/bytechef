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

package com.bytechef.component.ai.typesafe.util;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.ANSWERS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODELS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_ALIASES;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.NAME;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.QUESTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.QUESTION_NAME;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.USAGE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeUtils {

    private TypeSafeUtils() {
    }

    public static List<Option<String>> getModelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(http -> http.get("/v1/models"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Set<String> modelNames = new LinkedHashSet<>(MODEL_ALIASES);

        if (body != null && body.get(MODELS) instanceof List<?> models) {
            for (Object model : models) {
                if (model instanceof Map<?, ?> modelMap && modelMap.get(NAME) instanceof String name) {
                    modelNames.add(name);
                }
            }
        }

        List<Option<String>> options = new ArrayList<>();

        for (String modelName : modelNames) {
            options.add(option(modelName, modelName));
        }

        return options;
    }

    /**
     * Asks a single question against the input's state and returns its answer, flattened together with the resolved
     * model and the token usage so a workflow reads {@code ${typesafe_1.noul}} rather than digging through the
     * {@code answers} map of the raw response.
     */
    public static Map<String, Object> askQuestion(
        Parameters inputParameters, Map<String, Object> question, Context context) {

        Map<String, Object> requestBody = new LinkedHashMap<>();

        requestBody.put(MODEL, inputParameters.getRequiredString(MODEL));
        requestBody.put(QUESTIONS, Map.of(QUESTION_NAME, question));
        requestBody.put(STATE, toState(inputParameters.getRequired(STATE)));

        Map<String, Object> responseBody = context.http(http -> http.post("/v1/systemone"))
            .body(Body.of(requestBody))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (responseBody == null || !(responseBody.get(ANSWERS) instanceof Map<?, ?> answers) ||
            !(answers.get(QUESTION_NAME) instanceof Map<?, ?> answer)) {

            throw new IllegalStateException("TypeSafe response did not contain an answer: " + responseBody);
        }

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : answer.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        result.put(MODEL, responseBody.get(MODEL));
        result.put(USAGE, responseBody.get(USAGE));

        return result;
    }

    /**
     * The API accepts a string, an object, an array or null as state and answers 422 for a bare number or boolean, so
     * those are sent as their text instead.
     */
    private static Object toState(Object state) {
        if (state instanceof Number || state instanceof Boolean) {
            return String.valueOf(state);
        }

        return state;
    }
}
