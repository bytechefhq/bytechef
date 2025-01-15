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

package com.bytechef.component.ai.image.util;

import static com.bytechef.component.ai.image.constant.AiImageConstants.MODEL_PROVIDER;

import com.bytechef.component.ai.llm.azure.openai.constant.AzureOpenAiConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.constant.OpenAiConstants;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class AiImageAnalysisUtil {

    private AiImageAnalysisUtil() {
    }

    public static List<? extends Option<String>> getModelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Integer modelProvider = inputParameters.getInteger(MODEL_PROVIDER);

        return switch (modelProvider) {
            case 0 -> AzureOpenAiConstants.MODELS;
            case 1 -> OpenAiConstants.IMAGE_MODELS;
            default -> throw new IllegalStateException("Unexpected value: " + modelProvider);
        };
    }
}
