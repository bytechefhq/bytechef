/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JwtTokenServiceTest {

    private final CacheManager cacheManager = new ConcurrentMapCacheManager();
    private final JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(cacheManager);

    @Test
    void testCachedPublicKeyVerifiesTheToken() {
        String jwtToken = jwtTokenService.generateJwtToken("user-1", 7, 1, "public");

        AtomicReference<String> keyIdReference = new AtomicReference<>();

        Jws<Claims> jws = Jwts.parser()
            .keyLocator(header -> {
                keyIdReference.set((String) header.get("kid"));

                return jwtTokenService.getPublicKey(keyIdReference.get());
            })
            .build()
            .parseSignedClaims(jwtToken);

        Claims claims = jws.getPayload();

        assertThat(claims.getSubject()).isEqualTo("user-1");

        Cache cache = Objects.requireNonNull(cacheManager.getCache(JwtTokenServiceImpl.PUBLIC_KEYS_CACHE));

        assertThat(cache.get(keyIdReference.get(), Object.class)).isInstanceOf(String.class);
    }

    @Test
    void testUnknownKeyIdReturnsNull() {
        assertThat(jwtTokenService.getPublicKey("unknown")).isNull();
    }
}
