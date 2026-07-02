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

package com.bytechef.component.ai.llm.router.litellm.model;

import com.bytechef.component.ai.llm.router.model.RouterChatModel;
import java.util.Map;

/**
 * @author Aarish Yadav
 */
public class LiteLLMChatModel extends RouterChatModel {

    private LiteLLMChatModel(String baseUrl, Builder builder) {
        super(baseUrl, builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void addProviderSpecificParams(Map<String, Object> body) {
        if (reasoning != null) {
            body.put("reasoning", reasoning);
        }
    }

    public static class Builder extends RouterChatModel.Builder<Builder> {

        private String baseUrl;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;

            return self();
        }

        public LiteLLMChatModel build() {
            return new LiteLLMChatModel(baseUrl, this);
        }
    }
}
