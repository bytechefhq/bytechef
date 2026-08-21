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

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Configuration of the guest polyglot sandbox.
 *
 * <p>
 * Set {@code bytechef.script.sandbox.max-cpu-time} or {@code max-heap-memory} to an empty value to lift that ceiling;
 * set {@code bytechef.script.sandbox.enabled=false} to drop guest contexts back to the pre-sandbox {@code TRUSTED}
 * policy entirely. {@code bytechef.script.sandbox.max-concurrent-executions} sizes the platform-thread pool guest
 * executions run on - see {@code PolyglotGuestExecutor} for why they cannot run on the caller's virtual thread.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.script.sandbox")
public class PolyglotSandboxProperties {

    private boolean enabled = true;

    @Nullable
    private Duration maxCpuTime = PolyglotSandboxSettings.DEFAULT_MAX_CPU_TIME;

    @Nullable
    private DataSize maxHeapMemory = DataSize.ofBytes(PolyglotSandboxSettings.DEFAULT_MAX_HEAP_MEMORY);

    private int maxConcurrentExecutions = PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    public Duration getMaxCpuTime() {
        return maxCpuTime;
    }

    public void setMaxCpuTime(@Nullable Duration maxCpuTime) {
        this.maxCpuTime = maxCpuTime;
    }

    @Nullable
    public DataSize getMaxHeapMemory() {
        return maxHeapMemory;
    }

    public void setMaxHeapMemory(@Nullable DataSize maxHeapMemory) {
        this.maxHeapMemory = maxHeapMemory;
    }

    public int getMaxConcurrentExecutions() {
        return maxConcurrentExecutions;
    }

    public void setMaxConcurrentExecutions(int maxConcurrentExecutions) {
        this.maxConcurrentExecutions = maxConcurrentExecutions;
    }

    public PolyglotSandboxSettings toPolyglotSandboxSettings() {
        return new PolyglotSandboxSettings(
            enabled, maxCpuTime, maxHeapMemory == null ? null : maxHeapMemory.toBytes(), maxConcurrentExecutions);
    }
}
