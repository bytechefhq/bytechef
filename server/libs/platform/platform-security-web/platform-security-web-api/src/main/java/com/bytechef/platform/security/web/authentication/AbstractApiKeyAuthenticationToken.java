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
import java.util.Optional;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private long environmentId;
    private boolean environmentIdPresent;
    private String tenantId;
    private User user;

    public AbstractApiKeyAuthenticationToken() {
        super(List.of());
    }

    public AbstractApiKeyAuthenticationToken(long environmentId, String tenantId) {
        super(List.of());

        this.environmentId = environmentId;
        this.environmentIdPresent = true;
        this.tenantId = tenantId;
    }

    @SuppressFBWarnings("EI")
    public AbstractApiKeyAuthenticationToken(User user) {
        super(user.getAuthorities());

        this.user = user;

        setAuthenticated(true);
        setDetails(user);
    }

    /**
     * Authenticated, and retaining the environment the pre-authentication token was built for. The {@code User}-only
     * constructor above leaves {@link #environmentId} at its default of {@code 0}, which is a valid ordinal
     * ({@code DEVELOPMENT}) rather than an obviously-absent one -- so a provider that discards the environment on the
     * way to producing its authenticated token yields a principal that silently claims to be in DEVELOPMENT. Providers
     * whose environment matters downstream should use this instead.
     */
    @SuppressFBWarnings("EI")
    public AbstractApiKeyAuthenticationToken(long environmentId, User user) {
        super(user.getAuthorities());

        this.environmentId = environmentId;
        this.environmentIdPresent = true;
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

    /**
     * The environment this token was actually built for, or empty when it was built by a constructor that carries none.
     * {@link #getEnvironmentId()} cannot express that difference: it answers {@code 0} either way, and {@code 0} is a
     * valid ordinal ({@code DEVELOPMENT}), so a token that never received an environment is indistinguishable from one
     * genuinely in DEVELOPMENT.
     *
     * <p>
     * That distinction matters to anything deciding who a principal IS from its environment. Reading
     * {@link #getEnvironmentId()} there makes such a caller depend on every provider having remembered to carry the
     * environment into its authenticated token — a provider that forgets yields a confident wrong answer rather than a
     * missing one, and no reader can tell the two apart. Reading this instead makes a forgetful provider produce
     * "unknown", which every such caller can fail safely on. See ticket 1051's
     * {@code ConnectedUserResourceMembershipResolver}.
     */
    public Optional<Long> fetchEnvironmentId() {
        return environmentIdPresent ? Optional.of(environmentId) : Optional.empty();
    }
}
