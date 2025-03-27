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

package com.bytechef.ai.toolkit.tool;

import com.bytechef.ai.toolkit.Environment;
import java.util.Objects;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.client.RestClient;

public class ToolCallbackProviderFactory {

    private final String apiKey;
    private final RestClient.Builder restClientBuilder;

    private ToolCallbackProviderFactory(String apiKey, RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClientBuilder = restClientBuilder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ToolCallbackProvider create(String externalUserId, Environment environment) {
        return new ToolCallbackProviderImpl(externalUserId, apiKey, environment, restClientBuilder.build());
    }

    public static class Builder {

        private String apiKey;
        private RestClient.Builder restClientBuilder = RestClient.builder();

        private Builder() {
        }

        public Builder apiKey(String apiKey) {
            Objects.requireNonNull(apiKey, "apiKey cannot be null");

            this.apiKey = apiKey;

            return this;
        }

        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            Objects.requireNonNull(restClientBuilder, "restClientBuilder cannot be null");

            this.restClientBuilder = restClientBuilder;

            return this;
        }

        public ToolCallbackProviderFactory build() {
            if (apiKey == null || restClientBuilder == null) {
                throw new IllegalStateException("apiKey and restClientBuilder must be set");
            }

            return new ToolCallbackProviderFactory(apiKey, restClientBuilder);
        }
    }
}
