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

package com.bytechef.component.ai.llm.amazon.bedrock.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.regions.Region;

/**
 * @author Marko Kriskovic
 * @author Monika Kušter
 */
public final class AmazonBedrockConstants {

    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String REGION = "region";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .required(true);

    public static final List<Option<String>> REGION_OPTIONS = ModelUtils.getEnumOptions(
        Region.regions()
            .stream()
            .collect(Collectors.toMap(Region::id, Region::id)));

    private AmazonBedrockConstants() {
    }
}
