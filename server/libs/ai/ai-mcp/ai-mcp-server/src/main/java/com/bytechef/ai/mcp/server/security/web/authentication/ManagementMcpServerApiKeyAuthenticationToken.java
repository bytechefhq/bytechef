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

package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String authSecretKey;
    private String mcpServerSecretKey;

    public ManagementMcpServerApiKeyAuthenticationToken() {
        setAuthenticated(true);
    }

    public ManagementMcpServerApiKeyAuthenticationToken(String mcpServerSecretKey, String authSecretKey,
        String tenantId) {
        super(-1, tenantId);

        this.authSecretKey = authSecretKey;
        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getAuthSecretKey() {
        return authSecretKey;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
