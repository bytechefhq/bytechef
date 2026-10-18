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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * @author Ivica Cardic
 */
public class MultiIssuerJwtDecoder implements JwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(MultiIssuerJwtDecoder.class);

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

        try {
            return jwtDecoder.decode(token);
        } catch (BadJwtException badJwtException) {
            throw badJwtException;
        } catch (JwtException jwtException) {
            log.warn("Unable to validate a token of trusted issuer {}: {}", issuer, jwtException.getMessage());

            throw new BadJwtException(jwtException.getMessage(), jwtException);
        }
    }

    private JwtDecoder getJwtDecoder(String issuer) {
        JwtDecoder jwtDecoder = jwtDecoders.get(issuer);

        if (jwtDecoder != null) {
            return jwtDecoder;
        }

        JwtDecoder createdJwtDecoder;

        try {
            createdJwtDecoder = mcpJwtDecoderFactory.createJwtDecoder(issuer);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("Unable to load the configuration of trusted issuer {}: {}", issuer, exception.getMessage());

            throw new BadJwtException("Unable to load the configuration of issuer: " + issuer, exception);
        }

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
