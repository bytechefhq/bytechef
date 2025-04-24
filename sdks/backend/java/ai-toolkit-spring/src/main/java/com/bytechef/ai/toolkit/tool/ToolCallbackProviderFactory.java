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

package com.bytechef.ai.toolkit.tool;

import com.bytechef.ai.toolkit.Environment;
import org.springframework.ai.tool.ToolCallbackProvider;

public class ToolCallbackProviderFactory {

    private final String apiKey;
    private final String baseUrl;

    public ToolCallbackProviderFactory(String apiKey) {
        this(apiKey, "https://app.bytechef.io");
    }

    public ToolCallbackProviderFactory(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public ToolCallbackProvider create(String externalUserId, Environment environment) {
        return new ToolCallbackProviderImpl(apiKey, baseUrl, environment, externalUserId);
    }
}
