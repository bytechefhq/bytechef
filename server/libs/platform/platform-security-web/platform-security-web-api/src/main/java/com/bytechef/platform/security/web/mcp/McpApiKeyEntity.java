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

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * Adapts a ByteChef {@link ApiKey} to the mcp-security {@link ApiKeyEntity}. Also implements {@link UserDetails} so
 * {@code SecurityUtils.getCurrentUserLogin()} keeps resolving the key's owning user for auditing and authorization.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyEntity implements ApiKeyEntity, UserDetails {

    private final long apiKeyId;
    private final List<GrantedAuthority> authorities;
    private final Environment environment;
    private final String login;
    private @Nullable String secret;
    private final @Nullable PlatformType type;

    public McpApiKeyEntity(ApiKey apiKey, String login, List<GrantedAuthority> authorities) {
        Assert.notNull(apiKey.getId(), "'apiKey.id' must not be null");

        this.apiKeyId = apiKey.getId();
        this.authorities = List.copyOf(authorities);
        this.environment = apiKey.getEnvironment();
        this.login = login;
        this.secret = "{noop}" + apiKey.getSecretKey();
        this.type = apiKey.getType();
    }

    private McpApiKeyEntity(
        long apiKeyId, List<GrantedAuthority> authorities, Environment environment, String login,
        @Nullable String secret, @Nullable PlatformType type) {

        this.apiKeyId = apiKeyId;
        this.authorities = authorities;
        this.environment = environment;
        this.login = login;
        this.secret = secret;
        this.type = type;
    }

    @Override
    public String getId() {
        return login;
    }

    @Override
    @Nullable
    public String getSecret() {
        return secret;
    }

    @Override
    @SuppressFBWarnings("EI")
    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public void eraseCredentials() {
        secret = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ApiKeyEntity> T copy() {
        return (T) new McpApiKeyEntity(apiKeyId, authorities, environment, login, secret, type);
    }

    @Override
    @Nullable
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return login;
    }

    public long getApiKeyId() {
        return apiKeyId;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Nullable
    public PlatformType getType() {
        return type;
    }
}
