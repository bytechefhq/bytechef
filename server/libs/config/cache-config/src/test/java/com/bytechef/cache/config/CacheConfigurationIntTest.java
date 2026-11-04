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

import com.bytechef.cache.SharedCacheSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * @author Ivica Cardic
 */
@ExtendWith(OutputCaptureExtension.class)
class CacheConfigurationIntTest {

    private static final String REDIS_UNSUPPORTED_MESSAGE =
        "bytechef.cache.provider=redis requires ByteChef Enterprise Edition and an application that supports shared " +
            "caches; using caffeine";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CacheConfiguration.class);

    @Test
    void testStartsWithCaffeineProvider() {
        contextRunner
            .withPropertyValues("bytechef.cache.provider=caffeine")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
            });
    }

    @Test
    void testFallsBackToCaffeineWithRedisProviderWithoutSharedCacheSupport(CapturedOutput capturedOutput) {
        contextRunner
            .withPropertyValues("bytechef.cache.provider=redis")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
                assertThat(capturedOutput).contains(REDIS_UNSUPPORTED_MESSAGE);
            });
    }

    @Test
    void testFallsBackToCaffeineWithUpperCaseRedisProviderWithoutSharedCacheSupport(CapturedOutput capturedOutput) {
        contextRunner
            .withPropertyValues("bytechef.cache.provider=REDIS")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
                assertThat(capturedOutput).contains(REDIS_UNSUPPORTED_MESSAGE);
            });
    }

    @Test
    void testStartsWithRedisProviderWithSharedCacheSupport() {
        contextRunner
            .withPropertyValues("bytechef.cache.provider=redis")
            .withBean(SharedCacheSupport.class, () -> new SharedCacheSupport() {})
            .run(context -> assertThat(context).hasNotFailed());
    }
}
