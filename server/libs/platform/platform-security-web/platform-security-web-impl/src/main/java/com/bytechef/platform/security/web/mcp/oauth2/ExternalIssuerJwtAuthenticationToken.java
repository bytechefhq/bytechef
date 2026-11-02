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

package com.bytechef.platform.security.web.mcp.oauth2;

import com.bytechef.platform.security.web.authentication.ExternalIssuerAuthentication;
import java.io.Serial;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @author Ivica Cardic
 */
class ExternalIssuerJwtAuthenticationToken extends JwtAuthenticationToken implements ExternalIssuerAuthentication {

    @Serial
    private static final long serialVersionUID = 1L;

    ExternalIssuerJwtAuthenticationToken(
        Jwt jwt, UserDetails userDetails, Collection<? extends GrantedAuthority> authorities) {

        super(jwt, userDetails, authorities);
    }
}
