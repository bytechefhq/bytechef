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

package com.bytechef.message.broker.memory.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerMemory;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerMemory
public class MemoryMessageBrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MemoryMessageBrokerConfiguration.class);

    public MemoryMessageBrokerConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Message broker provider type enabled: memory");
        }
    }

    @Bean
    AsyncMessageBroker asyncMessageBroker(Environment environment) {
        return new AsyncMessageBroker(environment);
    }
}
