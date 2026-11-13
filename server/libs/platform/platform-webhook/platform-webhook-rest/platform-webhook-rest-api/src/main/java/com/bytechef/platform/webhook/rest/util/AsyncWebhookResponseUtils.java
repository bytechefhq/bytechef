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

package com.bytechef.platform.webhook.rest.util;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import java.util.Map;
import java.util.Set;

/**
 * Checks an immediate asynchronous webhook response before the workflow runs. Only the {@code REDIRECT} type may send
 * the caller elsewhere, so other types cannot use a redirect status code or set {@code Location} or {@code Set-Cookie}.
 *
 * @author Ivica Cardic
 */
public final class AsyncWebhookResponseUtils {

    private static final Set<String> FORBIDDEN_HEADER_NAMES = Set.of("location", "set-cookie");

    private AsyncWebhookResponseUtils() {
    }

    /**
     * Returns whether the response may be sent to the caller of an asynchronous webhook trigger.
     *
     * @param webhookResponse the configured response
     * @return true if the response is allowed, false otherwise
     */
    public static boolean isAllowed(WebhookResponse webhookResponse) {
        WebhookResponse.Type type = webhookResponse.getType();

        if (type == WebhookResponse.Type.REDIRECT) {
            return RedirectValidatorUtils.isValidAuthorRedirect(String.valueOf(webhookResponse.getBody()));
        }

        if (type == WebhookResponse.Type.BINARY) {
            return false;
        }

        int statusCode = webhookResponse.getStatusCode();

        if (statusCode >= 300 && statusCode < 400) {
            return false;
        }

        Map<String, String> headers = webhookResponse.getHeaders();

        for (String headerName : headers.keySet()) {
            if (FORBIDDEN_HEADER_NAMES.contains(headerName.toLowerCase())) {
                return false;
            }
        }

        return true;
    }
}
