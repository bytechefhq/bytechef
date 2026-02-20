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

package com.bytechef.server.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Configuration to track bean initialization times during startup. Enable with profile: startup-timing
 *
 * @author Ivica Cardic
 */
@Configuration
@Profile("startup-timing")
class StartupTimingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StartupTimingConfiguration.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    BeanPostProcessor beanTimingPostProcessor() {
        return new BeanTimingPostProcessor();
    }

    private static class BeanTimingPostProcessor implements BeanPostProcessor {

        private final Map<String, Long> beanStartTimes = new ConcurrentHashMap<>();
        private final Map<String, Long> beanDurations = new ConcurrentHashMap<>();

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            beanStartTimes.put(beanName, System.currentTimeMillis());

            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            Long startTime = beanStartTimes.remove(beanName);

            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;

                beanDurations.put(beanName, duration);

                if (duration > 100) {
                    logger.warn("SLOW BEAN: {} took {}ms to initialize", beanName, duration);
                }
            }

            return bean;
        }
    }
}
