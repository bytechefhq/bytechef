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

package com.bytechef.platform.scheduler.db.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Ivica Cardic
 */
public class DbSchedulerEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "dbSchedulerProvider";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty("bytechef.scheduler.provider", "quartz");

        boolean active = "db-scheduler".equals(provider);
        boolean uiEnabled = environment.getProperty("bytechef.scheduler.db-scheduler.ui.enabled", Boolean.class, true);

        Map<String, Object> source = new HashMap<>();

        source.put("db-scheduler.enabled", active);
        source.put("db-scheduler-ui.enabled", active && uiEnabled);

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, source));
    }
}
