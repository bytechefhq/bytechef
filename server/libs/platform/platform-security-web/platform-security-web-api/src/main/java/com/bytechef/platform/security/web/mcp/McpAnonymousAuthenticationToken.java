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

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
public final class McpAnonymousAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;

    private McpAnonymousAuthenticationToken(String principal) {
        super(List.of());

        this.principal = principal;

        setAuthenticated(true);
    }

    /**
     * @param principal identifies the anonymous caller, e.g. the server it reached; it must not carry a secret key
     */
    public static McpAnonymousAuthenticationToken of(String principal) {
        return new McpAnonymousAuthenticationToken(principal);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
