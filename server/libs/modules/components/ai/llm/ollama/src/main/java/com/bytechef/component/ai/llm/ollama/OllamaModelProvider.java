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

package com.bytechef.component.ai.llm.ollama;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.LLMModelProvider;
import com.bytechef.component.ai.llm.ollama.action.OllamaChatAction;
import com.bytechef.platform.ai.llm.Provider;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(LLMModelProvider.class)
public class OllamaModelProvider implements LLMModelProvider {

    @Override
    public Provider getProvider() {
        return Provider.OLLAMA;
    }

    @Override
    public ChatModel getChatModel() {
        return OllamaChatAction.CHAT_MODEL;
    }
}
