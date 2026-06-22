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

package com.bytechef.component.ai.llm;

import com.bytechef.platform.ai.llm.Provider;
import org.jspecify.annotations.Nullable;

/**
 * Self-registering provider of LLM chat and/or image models. Each LLM provider sub-module ships one
 * {@code @AutoService(LLMModelProvider.class)} implementation; the universal AI components discover them via
 * {@link LLMModelRegistry}.
 *
 * @author Ivica Cardic
 */
public interface LLMModelProvider {

    Provider getProvider();

    @Nullable
    default ChatModel getChatModel() {
        return null;
    }

    @Nullable
    default ImageModel getImageModel() {
        return null;
    }
}
