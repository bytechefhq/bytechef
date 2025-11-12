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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private long environmentId;
    private String tenantId;
    private User user;

    public AbstractApiKeyAuthenticationToken() {
        super(List.of());
    }

    public AbstractApiKeyAuthenticationToken(long environmentId, String tenantId) {
        super(List.of());

        this.environmentId = environmentId;
        this.tenantId = tenantId;
    }

    @SuppressFBWarnings("EI")
    public AbstractApiKeyAuthenticationToken(User user) {
        super(user.getAuthorities());

        this.user = user;

        setAuthenticated(true);
        setDetails(user);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    @SuppressFBWarnings("EI")
    public Object getPrincipal() {
        return user;
    }

    public String getTenantId() {
        return tenantId;
    }

    public long getEnvironmentId() {
        return environmentId;
    }
}
