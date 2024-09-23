/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.security.web.filter;

import com.bytechef.embedded.security.web.authentication.ConnectedUserAuthenticationToken;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationFilter;
import com.bytechef.platform.tenant.TenantKey;
import com.bytechef.platform.tenant.util.TenantUtils;
import com.bytechef.platform.user.service.SigningKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationFilter extends AbstractApiKeyAuthenticationFilter {

    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationFilter(
        AuthenticationManager authenticationManager, SigningKeyService signingKeyService) {

        super("/api/embedded/v([0-9]+)/by-connected-user-token/.+", authenticationManager);

        this.signingKeyService = signingKeyService;
    }

    @Override
    protected Authentication getAuthentication(HttpServletRequest request) {
        String token = getAuthToken(request);

        Environment environment = getEnvironment(request);

        Jws<Claims> jws = getJws(environment, token);

        Claims payload = jws.getPayload();

        String externalUserId = payload.getSubject();

        JwsHeader header = jws.getHeader();

        TenantKey tenantKey = TenantKey.parse(header.getKeyId());

        return new ConnectedUserAuthenticationToken(environment, externalUserId, tenantKey.getTenantId());
    }

    private Jws<Claims> getJws(Environment environment, String secretKey) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator(environment))
            .build()
            .parseSignedClaims(secretKey);
    }

    private class SigningKeyLocator implements Locator<Key> {

        private final Environment environment;

        private SigningKeyLocator(Environment environment) {
            this.environment = environment;
        }

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            TenantKey tenantKey = TenantKey.parse(keyId);

            return TenantUtils.callWithTenantId(
                tenantKey.getTenantId(), () -> signingKeyService.getPublicKey(keyId, environment));
        }
    }
}
