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

package com.bytechef.component.ai.universal.image.constant;

import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.STABILITY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.universal.image.util.AiImageUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.function.BiFunction;

/**
 * @author Marko Krišković
 */
public class AiImageConstants {

    public static final String GENERATE_IMAGE = "generateImage";

    public static final BiFunction<Ai.Provider, PropertyService, ModifiableStringProperty> PROVIDER_PROPERTY =
        (aiProvider, propertyService) -> string(PROVIDER)
            .label("Provider")
            .options(
                (ActionDefinition.OptionsFunction<String>) (
                    inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> AiImageUtils
                        .getProviderOptions(aiProvider, propertyService))

            .required(true);

    public static final ModifiableStringProperty MODEL_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options((ActionDefinition.OptionsFunction<String>) AiImageUtils::getModelOptions)
        .optionsLookupDependsOn(PROVIDER)
        .displayCondition("contains({'%s','%s'}, provider)".formatted(AZURE_OPEN_AI, OPEN_AI))
        .required(true);

    public static final ModifiableStringProperty MODEL_NO_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .displayCondition("provider == '%s'".formatted(STABILITY))
        .required(true);

    private AiImageConstants() {
    }
}
