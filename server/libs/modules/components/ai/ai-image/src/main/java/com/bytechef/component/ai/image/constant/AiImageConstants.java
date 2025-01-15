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

package com.bytechef.component.ai.image.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.image.util.AiImageAnalysisUtil;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource;

/**
 * @author Marko Krišković
 */
public class AiImageConstants {

    public static final String PROMPT = "prompt";

    public static final String MODEL_PROVIDER = "modelProvider";
    public static final String GENERATE_IMAGE = "generateImage";

    public static final ModifiableIntegerProperty MODEL_PROVIDER_PROPERTY = integer(MODEL_PROVIDER)
        .label("Model provider")
        .options(
            option("Azure Open AI", 0),
            option("Open AI", 1),
            option("Stability", 2))
        .required(true);

    public static final ModifiableStringProperty MODEL_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options((OptionsDataSource.ActionOptionsFunction<String>) AiImageAnalysisUtil::getModelOptions)
        .optionsLookupDependsOn(MODEL_PROVIDER)
        .displayCondition("modelProvider >= 0 && modelProvider <= 1")
        .required(true);

    public static final ModifiableStringProperty MODEL_NO_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .displayCondition("modelProvider == 2")
        .required(true);

    private AiImageConstants() {
    }
}
