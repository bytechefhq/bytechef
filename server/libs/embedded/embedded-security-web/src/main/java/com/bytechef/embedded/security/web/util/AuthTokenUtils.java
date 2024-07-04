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

package com.bytechef.embedded.security.web.util;

import com.bytechef.platform.constant.Environment;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * @author Ivica Cardic
 */
public class AuthTokenUtils {
    private static final String AUTH_TOKEN_HEADER_NAME = "Authorization";

    public static AuthToken getAuthToken(Pattern pathPattern, HttpServletRequest request) {
        String token = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (token == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        Environment environment;

        Matcher matcher = pathPattern.matcher(request.getRequestURI());

        if (matcher.find()) {
            String group = matcher.group(1);

            environment = Environment.valueOf(group.toUpperCase());
        } else {
            throw new BadCredentialsException("Unknown environment");
        }

        return new AuthToken(token, environment);
    }

    public record AuthToken(String token, Environment environment) {
    }
}
