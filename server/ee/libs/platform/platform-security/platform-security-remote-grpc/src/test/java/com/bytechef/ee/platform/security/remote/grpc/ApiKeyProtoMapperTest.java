/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class ApiKeyProtoMapperTest {

    @Test
    void testApiKeyRoundTrip() {
        ApiKey apiKey = new ApiKey();

        apiKey.setCreatedBy("admin@localhost.com");
        apiKey.setEnvironment(Environment.PRODUCTION);
        apiKey.setId(5L);
        apiKey.setLastUsedDate(Instant.parse("2026-10-26T08:00:00Z"));
        apiKey.setName("Production key");
        apiKey.setSecretKey("secret-key");
        apiKey.setType(PlatformType.EMBEDDED);
        apiKey.setUserId(1L);

        ApiKey mappedApiKey = ApiKeyProtoMapper.toApiKey(ApiKeyProtoMapper.toRemoteApiKey(apiKey));

        assertThat(mappedApiKey.getCreatedBy()).isEqualTo("admin@localhost.com");
        assertThat(mappedApiKey.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(mappedApiKey.getId()).isEqualTo(5L);
        assertThat(mappedApiKey.getLastUsedDate()).isEqualTo(Instant.parse("2026-10-26T08:00:00Z"));
        assertThat(mappedApiKey.getName()).isEqualTo("Production key");
        assertThat(mappedApiKey.getSecretKey()).isEqualTo("secret-key");
        assertThat(mappedApiKey.getType()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(mappedApiKey.getUserId()).isEqualTo(1L);
    }

    @Test
    void testApiKeyWithoutTypeRoundTrip() {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(Environment.DEVELOPMENT);
        apiKey.setSecretKey("secret-key");
        apiKey.setUserId(1L);

        ApiKey mappedApiKey = ApiKeyProtoMapper.toApiKey(ApiKeyProtoMapper.toRemoteApiKey(apiKey));

        assertThat(mappedApiKey.getType()).isNull();
        assertThat(mappedApiKey.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }
}
