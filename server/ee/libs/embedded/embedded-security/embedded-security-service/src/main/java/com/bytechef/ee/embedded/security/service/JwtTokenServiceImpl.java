/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.domain.TenantKey;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Date;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final long JWT_TTL_MILLIS = 10 * 60 * 1000;
    private static final KeyPairGenerator KEY_PAIR_GENERATOR;
    private static final String PUBLIC_KEYS_CACHE = JwtTokenService.class.getName() + ".publicKeys";

    private final CacheManager cacheManager;

    static {
        try {
            KEY_PAIR_GENERATOR = KeyPairGenerator.getInstance("RSA");

            KEY_PAIR_GENERATOR.initialize(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    JwtTokenServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public String generateJwtToken(String externalUserId, long integrationId, int environmentId, String tenantId) {
        Validate.notBlank(externalUserId, "externalUserId");
        Validate.notBlank(tenantId, "tenantId");

        TenantKey tenantKey = TenantKey.of(tenantId);

        String keyId = tenantKey.toString();

        KeyPair keyPair = generateKeyPair();

        Cache publicKeysCache = Validate.notNull(cacheManager.getCache(PUBLIC_KEYS_CACHE), PUBLIC_KEYS_CACHE);

        publicKeysCache.putIfAbsent(keyId, keyPair.getPublic());

        return Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .subject(externalUserId)
            .claim("environmentId", environmentId)
            .claim("integrationId", integrationId)
            .expiration(new Date(System.currentTimeMillis() + JWT_TTL_MILLIS))
            .signWith(keyPair.getPrivate())
            .compact();
    }

    @Override
    public @Nullable PublicKey getPublicKey(String keyId) {
        Cache publicKeysCache = Validate.notNull(cacheManager.getCache(PUBLIC_KEYS_CACHE), PUBLIC_KEYS_CACHE);

        return publicKeysCache.get(keyId, PublicKey.class);
    }

    private static KeyPair generateKeyPair() {
        return KEY_PAIR_GENERATOR.generateKeyPair();
    }
}
