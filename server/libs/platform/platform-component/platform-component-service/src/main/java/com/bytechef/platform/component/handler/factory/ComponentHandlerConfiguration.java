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

package com.bytechef.platform.component.handler.factory;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * @author Ivica Cardic
 */
@Configuration(proxyBeanMethods = false)
@Import(ComponentHandlerBeanRegistrar.class)
class ComponentHandlerConfiguration {

    @Async
    @EventListener(ContextRefreshedEvent.class)
    void onContextRefreshed() {
        // ComponentHandlerBeanRegistrar.COMPONENT_HANDLER_ENTRIES_SUPPLIER.get();
    }
}
