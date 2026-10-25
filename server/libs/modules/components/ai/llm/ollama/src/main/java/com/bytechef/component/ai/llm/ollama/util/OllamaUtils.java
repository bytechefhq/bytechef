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

package com.bytechef.component.ai.llm.ollama.util;

import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.definition.Parameters;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ivica Cardic
 */
public final class OllamaUtils {

    private OllamaUtils() {
    }

    public static OllamaApi createOllamaApi(Parameters connectionParameters) {
        OllamaApi.Builder ollamaApiBuilder = OllamaApi.builder();

        String url = connectionParameters.getString(URL);

        if (url != null && !url.isEmpty()) {
            ollamaApiBuilder.baseUrl(url);
        }

        String token = connectionParameters.getString(TOKEN);

        if (token != null && !token.isEmpty()) {
            String authorization = "Bearer " + token;

            ollamaApiBuilder.restClientBuilder(
                RestClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, authorization));
            ollamaApiBuilder.webClientBuilder(
                WebClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, authorization));
        }

        return ollamaApiBuilder.build();
    }
}
