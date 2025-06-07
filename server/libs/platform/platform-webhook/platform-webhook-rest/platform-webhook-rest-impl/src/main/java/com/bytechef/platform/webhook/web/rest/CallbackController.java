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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.config.ApplicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class CallbackController {

    private final String redirectUri;

    public CallbackController(ApplicationProperties applicationProperties) {
        this.redirectUri = "redirect:%s/oauth.html".formatted(
            applicationProperties.getOauth2()
                .getRedirectUri()
                .replace("/callback", ""));
    }

    @GetMapping("/callback")
    public String handleCallback(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder(redirectUri);

        Map<String, String[]> parameterMap = request.getParameterMap();

        String state = null;

        if (!parameterMap.isEmpty()) {
            sb.append('?');
            boolean first = true;

            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();

                for (String value : values) {
                    if (!first) {
                        sb.append('&');
                    }

                    sb.append(key)
                        .append('=')
                        .append(value);

                    first = false;
                }

                if (key.equals("state")) {
                    state = values[0];
                }
            }
        }

        if (state == null) {
            throw new IllegalStateException("No state parameter found in callback");
        }

        return sb.toString();
    }
}
