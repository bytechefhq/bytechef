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
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * A {@link JwtDecoder} that supports multiple trusted issuers. It reads the (unverified) {@code iss} claim of the
 * presented token, selects the {@link JwtDecoder} for that issuer, and delegates full validation (signature, issuer,
 * audience, expiry) to it. A token whose issuer is not trusted is rejected before any signature check.
 *
 * <p>
 * Selecting the per-issuer decoder from the unverified {@code iss} is safe: the chosen decoder itself validates that
 * the token was signed by that issuer's keys and that its {@code iss} matches, so a token that merely claims a trusted
 * issuer but is signed by someone else is rejected.
 *
 * @author Ivica Cardic
 */
public class MultiIssuerJwtDecoder implements JwtDecoder {

    private final Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();
    private final Function<String, @Nullable JwtDecoder> jwtDecoderFactory;

    public MultiIssuerJwtDecoder(Function<String, @Nullable JwtDecoder> jwtDecoderFactory) {
        this.jwtDecoderFactory = jwtDecoderFactory;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = extractIssuer(token);

        if (issuer == null) {
            throw new BadJwtException("Missing issuer claim");
        }

        JwtDecoder jwtDecoder = jwtDecoders.computeIfAbsent(issuer, this::createJwtDecoder);

        if (jwtDecoder == NULL_DECODER) {
            throw new BadJwtException("Untrusted issuer: " + issuer);
        }

        return jwtDecoder.decode(token);
    }

    private JwtDecoder createJwtDecoder(String issuer) {
        JwtDecoder jwtDecoder = jwtDecoderFactory.apply(issuer);

        return jwtDecoder == null ? NULL_DECODER : jwtDecoder;
    }

    private static String extractIssuer(String token) {
        try {
            return JWTParser.parse(token)
                .getJWTClaimsSet()
                .getIssuer();
        } catch (ParseException parseException) {
            throw new BadJwtException("Malformed JWT", parseException);
        }
    }

    /**
     * Sentinel cached for untrusted issuers so a rejected issuer is not re-resolved through the factory on every
     * request.
     */
    private static final JwtDecoder NULL_DECODER = token -> {
        throw new BadJwtException("Untrusted issuer");
    };
}
