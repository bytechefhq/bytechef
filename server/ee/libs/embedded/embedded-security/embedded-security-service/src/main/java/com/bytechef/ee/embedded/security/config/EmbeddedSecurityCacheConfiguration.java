/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.config;

import com.bytechef.cache.SharedCaches;
import com.bytechef.ee.embedded.security.service.JwtTokenServiceImpl;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
class EmbeddedSecurityCacheConfiguration {

    @Bean
    SharedCaches jwtTokenSharedCaches() {
        return SharedCaches.of(JwtTokenServiceImpl.PUBLIC_KEYS_CACHE);
    }
}
