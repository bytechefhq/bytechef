/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.config;

import com.bytechef.cache.SharedCaches;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobServiceImpl;
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
class ApiConnectorGenerationJobCacheConfiguration {

    @Bean
    SharedCaches apiConnectorGenerationJobSharedCaches() {
        return SharedCaches.of(ApiConnectorGenerationJobServiceImpl.JOBS_CACHE);
    }
}
