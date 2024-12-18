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

package com.bytechef.component.anthropic.constant;

import com.bytechef.component.definition.Option;
import com.bytechef.component.llm.util.LLMUtils;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.anthropic.api.AnthropicApi;

/**
 * @author Ivica Cardic
 */
public class AnthropicConstants {
    public static final List<Option<String>> MODELS = LLMUtils
        .getEnumOptions(Arrays.stream(AnthropicApi.ChatModel.values())
            .collect(
                Collectors.toMap(
                    AnthropicApi.ChatModel::getValue, AnthropicApi.ChatModel::getValue, (f, s) -> f)));
}
