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

package com.bytechef.platform.component.polyglot;

import com.bytechef.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * @author Ivica Cardic
 */
@Configuration
public class PolyglotSandboxConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PolyglotSandboxConfiguration.class);

    private final PolyglotSandboxSettings polyglotSandboxSettings;

    public PolyglotSandboxConfiguration(ApplicationProperties applicationProperties) {
        ApplicationProperties.Script.Sandbox sandbox = applicationProperties.getScript()
            .getSandbox();

        DataSize maxHeapMemory = sandbox.getMaxHeapMemory();

        this.polyglotSandboxSettings = new PolyglotSandboxSettings(
            sandbox.isEnabled(), sandbox.getMaxCpuTime(), maxHeapMemory == null ? null : maxHeapMemory.toBytes(),
            sandbox.getMaxConcurrentExecutions());
    }

    PolyglotSandboxSettings getPolyglotSandboxSettings() {
        return polyglotSandboxSettings;
    }

    @PostConstruct
    public void applyPolyglotSandboxSettings() {
        PolyglotSandbox.setSettings(polyglotSandboxSettings);

        if (!polyglotSandboxSettings.enabled()) {
            log.warn("Guest polyglot sandboxing is disabled; scripts run without resource limits");
        } else if (log.isInfoEnabled()) {
            log.info(
                "Guest polyglot sandbox enabled: maxCpuTime={}, maxHeapMemory={}",
                polyglotSandboxSettings.maxCpuTime(), polyglotSandboxSettings.maxHeapMemory());
        }
    }
}
