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

package com.bytechef.platform.ratelimit.config;

import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.Bucket4jRateLimiter;
import com.bytechef.platform.ratelimit.ConcurrentExecutionGate;
import com.bytechef.platform.ratelimit.InMemoryConcurrentExecutionGate;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.platform.ratelimit.RateLimiter;
import com.bytechef.platform.ratelimit.RedisConcurrentExecutionGate;
import com.bytechef.platform.ratelimit.RedisRateLimiter;
import com.bytechef.platform.ratelimit.web.PlanRateLimitFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Plan-limit enforcement wiring. {@code bytechef.plan.enforcement.enabled=true} by default — but with the SELF_HOSTED
 * default plan every limit is {@code null}, so the filter and gate no-op until a tier is configured; existing
 * deployments see zero behavior change. Disable outright with {@code bytechef.plan.enforcement.enabled=false}.
 *
 * <p>
 * {@code bytechef.plan.enforcement.provider} selects the backing store: {@code local} (default) keeps per-node
 * in-memory buckets and slot counters — an HA deployment over-admits by the node count at worst — while {@code redis}
 * moves both onto shared Redis state (requires a {@code RedisConnectionFactory} bean) for strict global limits.
 * </p>
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "bytechef.plan.enforcement", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlanRateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RateLimiter rateLimiter() {
        return new Bucket4jRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    PlanLimitRejectionCounter planLimitRejectionCounter(ObjectProvider<MeterRegistry> meterRegistryObjectProvider) {
        return new PlanLimitRejectionCounter(meterRegistryObjectProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    ConcurrentExecutionGate concurrentExecutionGate() {
        return new InMemoryConcurrentExecutionGate();
    }

    /**
     * Registered before the enclosing class's fallback beans (nested configurations are processed first), so when the
     * provider is {@code redis} and a connection factory exists, the {@code @ConditionalOnMissingBean} locals back off.
     */
    @Configuration
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "bytechef.plan.enforcement", name = "provider", havingValue = "redis")
    static class RedisPlanEnforcementConfiguration {

        @Bean
        RateLimiter redisRateLimiter(RedisConnectionFactory redisConnectionFactory) {
            return new RedisRateLimiter(new StringRedisTemplate(redisConnectionFactory));
        }

        @Bean
        ConcurrentExecutionGate redisConcurrentExecutionGate(RedisConnectionFactory redisConnectionFactory) {
            return new RedisConcurrentExecutionGate(new StringRedisTemplate(redisConnectionFactory));
        }
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnBean(PlanLimitsProvider.class)
    FilterRegistrationBean<PlanRateLimitFilter> planRateLimitFilter(
        PlanLimitRejectionCounter planLimitRejectionCounter, PlanLimitsProvider planLimitsProvider,
        RateLimiter rateLimiter) {

        FilterRegistrationBean<PlanRateLimitFilter> filterRegistrationBean = new FilterRegistrationBean<>(
            new PlanRateLimitFilter(planLimitRejectionCounter, planLimitsProvider, rateLimiter));

        // After the security filter chain (default order -100) so isAnonymous() sees the outcome of authentication,
        // but before the dispatcher servlet work begins.
        filterRegistrationBean.setOrder(0);

        return filterRegistrationBean;
    }
}
