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

package com.bytechef.ee.observability.env;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Matija Petanjek
 */
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        if (!environment.getProperty("bytechef.observability.enabled", Boolean.class, false)) {
            source.put(
                "spring.autoconfigure.exclude",
                StringUtils.join(
                    environment.getProperty("spring.autoconfigure.exclude"),
                    """
                        ,org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
                        ,org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration
                        ,org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration
                        ,org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration
                        ,org.springframework.boot.actuate.autoconfigure.logging.OpenTelemetryLoggingAutoConfiguration
                        """));

            source.put("bytechef.observability.loki.appender.level", "OFF");
        }

        MapPropertySource mapPropertySource = new MapPropertySource("Custom Observability Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
