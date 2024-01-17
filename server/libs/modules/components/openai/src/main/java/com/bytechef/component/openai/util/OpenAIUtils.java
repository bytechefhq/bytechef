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

package com.bytechef.component.openai.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_2;
import static com.bytechef.component.openai.constant.OpenAIConstants.DEFAULT_SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OutputSchema;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class OpenAIUtils {

    public static final ObjectProperty outputSchemaResponseForStream =
        object()
            .properties(
                array("stream")
                    .items(
                        string("id"),
                        string("object"),
                        integer("created"),
                        string("model"),
                        array("choices")
                            .items(
                                object()
                                    .properties(
                                        integer("index"),
                                        object("message")
                                            .properties(
                                                string("role"),
                                                string("content"),
                                                string("name"),
                                                object("functionCall")
                                                    .properties(
                                                        string("name"),
                                                        object("arguments"))),
                                        string("finishReason")))));

    public static final ObjectProperty outputSchemaResponse =
        object()
            .properties(
                string("id"),
                string("object"),
                integer("created"),
                string("model"),
                array("choices")
                    .items(
                        object()
                            .properties(
                                integer("index"),
                                object("message")
                                    .properties(
                                        string("role"),
                                        string("content"),
                                        string("name"),
                                        object("functionCall")
                                            .properties(
                                                string("name"),
                                                object("arguments"))),
                                string("finishReason"))),
                object("usage")
                    .properties(
                        integer("promptTokens"),
                        integer("completionTokens"),
                        integer("totalTokens")));

    private OpenAIUtils() {
    }

    public static List<Option<String>> getSizeOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {
        String model = inputParameters.getRequiredString(MODEL);

        List<Option<String>> options = new ArrayList<>();

        if (model.equals(DALL_E_2)) {
            options.add(option("256x256", "256x256"));
            options.add(option("512x512", "512x512"));
            options.add(option(DEFAULT_SIZE, DEFAULT_SIZE));
        } else {
            options.add(option(DEFAULT_SIZE, DEFAULT_SIZE));
            options.add(option("1792x1024", "1792x1024"));
            options.add(option("1024x1792", "1024x1792"));
        }

        return options;
    }

    public static List<ValueProperty<?>> getModelProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String model = inputParameters.getRequiredString(MODEL);

        ModifiableStringProperty string = string(PROMPT)
            .label("Prompt")
            .description("A text description of the desired image(s).")
            .required(true);

        ModifiableIntegerProperty n = integer(N)
            .label("n")
            .description(
                "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.")
            .defaultValue(1)
            .required(false);

        if (model.equals(DALL_E_2)) {
            string.maxLength(1000);
            n.minValue(1)
                .maxValue(10);
        } else {
            string.maxLength(4000);
        }

        return List.of(string, n);
    }

    public static OutputSchema getOutputSchema(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        boolean stream = inputParameters.getRequiredBoolean(STREAM);

        if (stream) {
            return new OutputSchema(outputSchemaResponseForStream);
        } else {
            return new OutputSchema(outputSchemaResponse);
        }
    }
}
