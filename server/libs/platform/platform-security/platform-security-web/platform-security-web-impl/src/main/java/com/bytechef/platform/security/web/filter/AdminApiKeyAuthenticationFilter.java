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

package com.bytechef.platform.security.web.filter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * @author Ivica Cardic
 */
public class AdminApiKeyAuthenticationFilter extends AbstractApiKeyAuthenticationFilter {

    @SuppressFBWarnings("EI")
    public AdminApiKeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(
            "^/api/platform/v([0-9]+)/.+", authenticationManager,
            AdminApiKeyAuthenticationFilter::getUrlItems);
    }

    private static UrlItems getUrlItems(Pattern pathPattern, HttpServletRequest request) {
        Matcher matcher = pathPattern.matcher(request.getRequestURI());

        int version = 0;

        if (matcher.find()) {
            String group = matcher.group(1);

            version = Integer.parseInt(group);
        }

        return new UrlItems(null, version);
    }
}
