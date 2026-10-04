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

import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * @author Ivica Cardic
 */
public class MultiIssuerJwtDecoder implements JwtDecoder {

    private final Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();
    private final McpJwtDecoderFactory mcpJwtDecoderFactory;

    public MultiIssuerJwtDecoder(McpJwtDecoderFactory mcpJwtDecoderFactory) {
        this.mcpJwtDecoderFactory = mcpJwtDecoderFactory;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = extractIssuer(token);

        if (issuer == null) {
            throw new BadJwtException("Missing issuer claim");
        }

        if (!mcpJwtDecoderFactory.isTrustedIssuer(issuer)) {
            throw new BadJwtException("Untrusted issuer: " + issuer);
        }

        JwtDecoder jwtDecoder = getJwtDecoder(issuer);

        return jwtDecoder.decode(token);
    }

    private JwtDecoder getJwtDecoder(String issuer) {
        JwtDecoder jwtDecoder = jwtDecoders.get(issuer);

        if (jwtDecoder != null) {
            return jwtDecoder;
        }

        JwtDecoder createdJwtDecoder = mcpJwtDecoderFactory.createJwtDecoder(issuer);

        if (createdJwtDecoder == null) {
            throw new BadJwtException("Untrusted issuer: " + issuer);
        }

        JwtDecoder cachedJwtDecoder = jwtDecoders.putIfAbsent(issuer, createdJwtDecoder);

        return cachedJwtDecoder == null ? createdJwtDecoder : cachedJwtDecoder;
    }

    @Nullable
    private static String extractIssuer(String token) {
        try {
            return JWTParser.parse(token)
                .getJWTClaimsSet()
                .getIssuer();
        } catch (ParseException parseException) {
            throw new BadJwtException("Malformed JWT", parseException);
        }
    }
}
