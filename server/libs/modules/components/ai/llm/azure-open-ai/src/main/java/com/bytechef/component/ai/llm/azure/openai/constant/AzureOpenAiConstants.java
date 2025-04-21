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

package com.bytechef.component.ai.llm.azure.openai.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;

public class AzureOpenAiConstants {

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("Deployment name, written in string.")
        .exampleValue("gpt-4o")
        .required(true);

    public static final List<Option<String>> IMAGE_MODELS = ModelUtils.getEnumOptions(
        Arrays.stream(AzureOpenAiImageOptions.ImageModel.values())
            .collect(
                Collectors.toMap(
                    AzureOpenAiImageOptions.ImageModel::getValue,
                    AzureOpenAiImageOptions.ImageModel::getValue)));

    private AzureOpenAiConstants() {
    }
}
