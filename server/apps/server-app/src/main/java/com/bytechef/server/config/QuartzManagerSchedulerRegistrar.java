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

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;

/**
 * Makes the scheduler created by Spring Boot's Quartz auto configuration reachable under the bean name Quartz Manager
 * looks for.
 *
 * <p>
 * Quartz Manager injects its scheduler with {@code @Qualifier("quartzManagerScheduler")}, while Spring Boot registers
 * the application scheduler as {@code quartzScheduler}. Registering an alias, rather than declaring a second
 * {@code Scheduler} bean that returns the same instance, keeps the scheduler unambiguous for the by type injection
 * points in {@code QuartzSchedulerConfiguration}.
 *
 * @author Ivica Cardic
 */
class QuartzManagerSchedulerRegistrar implements BeanRegistrar {

    private static final String QUARTZ_MANAGER_SCHEDULER_BEAN_NAME = "quartzManagerScheduler";
    private static final String QUARTZ_SCHEDULER_BEAN_NAME = "quartzScheduler";

    @Override
    public void register(BeanRegistry registry, Environment environment) {
        registry.registerAlias(QUARTZ_SCHEDULER_BEAN_NAME, QUARTZ_MANAGER_SCHEDULER_BEAN_NAME);
    }
}
