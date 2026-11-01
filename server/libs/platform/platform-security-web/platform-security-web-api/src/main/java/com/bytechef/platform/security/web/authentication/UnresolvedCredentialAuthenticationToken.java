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

package com.bytechef.platform.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * @author Ivica Cardic
 */
public final class UnresolvedCredentialAuthenticationToken extends AbstractAuthenticationToken {

    public static final String PRINCIPAL = "[unresolved credential]";

    public UnresolvedCredentialAuthenticationToken(HttpServletRequest httpServletRequest) {
        super(List.of());

        setDetails(new WebAuthenticationDetails(httpServletRequest));
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("An unresolved credential cannot be authenticated");
        }

        super.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return PRINCIPAL;
    }
}
