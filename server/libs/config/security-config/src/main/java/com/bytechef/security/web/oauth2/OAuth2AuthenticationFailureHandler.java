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

package com.bytechef.security.web.oauth2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * OAuth2 authentication failure handler. Redirects to the login page with an error indicator and, when the failure
 * carries one, the OAuth2 error code so the page can tell "no account" apart from a provider error.
 *
 * @author Ivica Cardic
 */
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    static final String LOGIN_ERROR_URL = "/login?error=oauth2";

    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("[a-z_]{1,64}");

    @Override
    @SuppressFBWarnings(
        value = "UNVALIDATED_REDIRECT",
        justification = "The Location is this application's login page; only a [a-z_] error code is appended")
    public void onAuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
        throws IOException {

        String redirectUrl = LOGIN_ERROR_URL;

        if (exception instanceof OAuth2AuthenticationException oAuth2AuthenticationException) {
            String errorCode = oAuth2AuthenticationException.getError()
                .getErrorCode();

            if (errorCode != null && ERROR_CODE_PATTERN.matcher(errorCode)
                .matches()) {

                redirectUrl += "&code=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
