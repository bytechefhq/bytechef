/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.config;

import com.integri.atlas.engine.coordinator.event.EventListener;
import com.integri.atlas.engine.coordinator.event.EventListenerChain;
import com.integri.atlas.engine.coordinator.event.LogEventListener;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EventsConfiguration {

    @Bean
    @Primary
    EventListenerChain eventListener(List<EventListener> aEventListeners) {
        return new EventListenerChain(aEventListeners);
    }

    @Bean
    LogEventListener logEventListener() {
        return new LogEventListener();
    }
}
