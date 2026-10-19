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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public class McpServerDisabledException extends AuthenticationException {

    public static final String A2A_SERVER_MESSAGE = "This A2A server is disabled. Enable it in ByteChef to use it.";

    public static final String MCP_SERVER_MESSAGE = "This MCP server is disabled. Enable it in ByteChef to use it.";

    private final String responseMessage;

    public McpServerDisabledException(String message) {
        super(message);

        this.responseMessage = message;
    }

    public void writeResponse(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String body = "{\"error\":\"server_disabled\",\"message\":\"" + escapeJson(responseMessage) + "\"}";

        httpServletResponse.getOutputStream()
            .write(body.getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}
