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

package com.bytechef.cache.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.NoOpCache;

/**
 * @author Ivica Cardic
 */
class CacheConfigurationTest {

    private static final String WORKFLOW_CACHE = "com.bytechef.atlas.configuration.service.WorkflowService.workflow";
    private static final String WORKFLOWS_CACHE =
        "com.bytechef.atlas.configuration.service.WorkflowService.workflows";

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CacheConfiguration.class);

    @Test
    void testCommunityEditionWebhookNodeDoesNotCacheWorkflows() {
        applicationContextRunner
            .withPropertyValues("bytechef.edition=ce", "bytechef.coordinator.enabled=false")
            .run(context -> {
                CacheManager cacheManager = context.getBean(CacheManager.class);

                assertThat(getCache(cacheManager, WORKFLOW_CACHE)).isInstanceOf(NoOpCache.class);
                assertThat(getCache(cacheManager, WORKFLOWS_CACHE)).isInstanceOf(NoOpCache.class);
                assertThat(getCache(cacheManager, "other")).isInstanceOf(CaffeineCache.class);
            });
    }

    @Test
    void testCommunityEditionWebhookNodeIsDetectedWithoutAnEditionProperty() {
        applicationContextRunner
            .withPropertyValues("bytechef.coordinator.enabled=false")
            .run(context -> assertThat(getCache(context.getBean(CacheManager.class), WORKFLOW_CACHE))
                .isInstanceOf(NoOpCache.class));
    }

    @Test
    void testCommunityEditionCoordinatorCachesWorkflows() {
        applicationContextRunner
            .withPropertyValues("bytechef.edition=ce")
            .run(context -> assertThat(getCache(context.getBean(CacheManager.class), WORKFLOW_CACHE))
                .isInstanceOf(CaffeineCache.class));
    }

    @Test
    void testCommunityEditionWorkerCachesWorkflows() {
        applicationContextRunner
            .withPropertyValues(
                "bytechef.edition=ce", "bytechef.coordinator.enabled=false", "bytechef.webhook.enabled=false")
            .run(context -> assertThat(getCache(context.getBean(CacheManager.class), WORKFLOW_CACHE))
                .isInstanceOf(CaffeineCache.class));
    }

    @Test
    void testEnterpriseEditionWebhookNodeCachesWorkflows() {
        applicationContextRunner
            .withPropertyValues("bytechef.edition=EE", "bytechef.coordinator.enabled=false")
            .run(context -> assertThat(getCache(context.getBean(CacheManager.class), WORKFLOW_CACHE))
                .isInstanceOf(CaffeineCache.class));
    }

    private static Cache getCache(CacheManager cacheManager, String name) {
        return Objects.requireNonNull(cacheManager.getCache(name), name);
    }
}
