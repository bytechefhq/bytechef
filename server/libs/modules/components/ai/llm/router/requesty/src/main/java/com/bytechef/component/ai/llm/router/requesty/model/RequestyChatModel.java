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

package com.bytechef.component.ai.llm.router.requesty.model;

import static com.bytechef.component.ai.llm.router.requesty.constant.RequestyConstants.BASE_URL;

import com.bytechef.component.ai.llm.router.model.RouterChatModel;
import java.util.Map;

/**
 * @author Thibault Jaigu
 */
public class RequestyChatModel extends RouterChatModel {

    private RequestyChatModel(Builder builder) {
        super(BASE_URL, builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void addProviderSpecificParams(Map<String, Object> body) {
        if (reasoning != null) {
            body.put("reasoning_effort", reasoning);
        }
    }

    public static class Builder extends RouterChatModel.Builder<Builder> {

        public RequestyChatModel build() {
            return new RequestyChatModel(this);
        }
    }
}
