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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Custom OidcUser implementation that wraps the internal ByteChef user login. The {@link #getName()} method returns the
 * internal user's login, which is critical for remember-me compatibility. Delegates all OIDC-specific methods to the
 * wrapped OidcUser.
 *
 * @author Ivica Cardic
 */
public class CustomOidcUser implements OidcUser {

    private final Collection<? extends GrantedAuthority> authorities;
    private final String login;
    private final OidcUser oidcUser;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CustomOidcUser(String login, Collection<? extends GrantedAuthority> authorities, OidcUser oidcUser) {
        this.login = login;
        this.authorities = List.copyOf(authorities);
        this.oidcUser = oidcUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public String getName() {
        return login;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }
}
