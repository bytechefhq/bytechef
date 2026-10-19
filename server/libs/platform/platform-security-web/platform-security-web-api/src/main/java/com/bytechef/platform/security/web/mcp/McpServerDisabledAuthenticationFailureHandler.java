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

package com.bytechef.platform.security.web.mcp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * @author Ivica Cardic
 */
public class McpServerDisabledAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuthenticationFailureHandler authenticationFailureHandler;

    public McpServerDisabledAuthenticationFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        AuthenticationException authenticationException) throws IOException, ServletException {

        if (authenticationException instanceof McpServerDisabledException mcpServerDisabledException) {
            mcpServerDisabledException.writeResponse(httpServletResponse);

            return;
        }

        authenticationFailureHandler.onAuthenticationFailure(
            httpServletRequest, httpServletResponse, authenticationException);
    }
}
