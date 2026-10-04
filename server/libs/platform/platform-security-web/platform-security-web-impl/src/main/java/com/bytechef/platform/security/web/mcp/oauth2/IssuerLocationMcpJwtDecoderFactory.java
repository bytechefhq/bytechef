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

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

/**
 * @author Ivica Cardic
 */
public class IssuerLocationMcpJwtDecoderFactory implements McpJwtDecoderFactory {

    private final Set<String> trustedIssuerUris;

    public IssuerLocationMcpJwtDecoderFactory(Collection<String> trustedIssuerUris) {
        this.trustedIssuerUris = Set.copyOf(trustedIssuerUris);
    }

    @Override
    @Nullable
    public JwtDecoder createJwtDecoder(String issuer) {
        if (!isTrustedIssuer(issuer)) {
            return null;
        }

        return JwtDecoders.fromIssuerLocation(issuer);
    }

    @Override
    public boolean isTrustedIssuer(String issuer) {
        return trustedIssuerUris.contains(issuer);
    }
}
