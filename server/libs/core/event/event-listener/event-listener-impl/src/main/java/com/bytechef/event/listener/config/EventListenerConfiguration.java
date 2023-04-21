
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.event.listener.config;

import com.bytechef.event.listener.EventListener;
import com.bytechef.event.listener.EventListenerChain;
import com.bytechef.event.listener.LogEventListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(EventListenerProperties.class)
public class EventListenerConfiguration {

    @Bean
    @Primary
    EventListener eventListener(List<EventListener> eventListeners) {
        return new EventListenerChain(eventListeners);
    }

    @Bean
    LogEventListener logEventListener() {
        return new LogEventListener();
    }
}
