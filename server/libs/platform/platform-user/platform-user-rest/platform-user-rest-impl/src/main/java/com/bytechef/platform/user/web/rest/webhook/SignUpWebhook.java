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

package com.bytechef.platform.user.web.rest.webhook;

import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@Component
public class SignUpWebhook {

    private final Resource resource;
    private final RestClient restClient;
    private final String webhookCsrfToken;

    public SignUpWebhook(
        @Value("${webhook.sign-up.url:''}") Resource resource,
        @Value("${webhook.sign-up.csrf-token:''}") String webhookCsrfToken, RestClient.Builder restClientBuilder) {

        this.resource = resource;
        this.webhookCsrfToken = webhookCsrfToken;
        this.restClient = restClientBuilder.build();
    }

    public boolean isEmailDomainValid(String email) {
        if (resource == null || !resource.exists()) {
            return true;
        } else {
            try {
                Result result = restClient.post()
                    .uri(resource.getURI())
                    .header("X-CSRF-TOKEN", webhookCsrfToken)
                    .body(Map.of("email", email))
                    .retrieve()
                    .body(Result.class);

                return Validate.notNull(result, "Result is null")
                    .valid();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private record Result(boolean valid) {
    }
}
