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

package com.bytechef.platform.component.runner;

import com.bytechef.config.ApplicationProperties;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.unit.DataSize;

/**
 * Resolves the operator-set ceilings for one task runner type: the global {@code bytechef.script.runner-limits} values,
 * overridden per runner by that runner's own properties.
 */
public final class TaskRunnerLimits {

    public static final String MAX_LOG_BYTES_PROPERTY = "max-log-bytes";
    public static final String MAX_TRANSFER_BYTES_PROPERTY = "max-transfer-bytes";
    public static final String MAX_WAIT_PROPERTY = "max-wait";

    private static final String GLOBAL_PREFIX = "bytechef.script.runner-limits.";

    private static final Duration DEFAULT_MAX_WAIT = Duration.ofHours(1);
    private static final long DEFAULT_MAX_LOG_BYTES = 1024L * 1024;
    private static final long DEFAULT_MAX_TRANSFER_BYTES = 512L * 1024 * 1024;

    private final Duration maxWait;
    private final long maxLogBytes;
    private final long maxTransferBytes;

    private TaskRunnerLimits(Duration maxWait, long maxLogBytes, long maxTransferBytes) {
        this.maxWait = maxWait;
        this.maxLogBytes = maxLogBytes;
        this.maxTransferBytes = maxTransferBytes;
    }

    public static TaskRunnerLimits of(ApplicationProperties applicationProperties, String runnerType) {
        ApplicationProperties.Script script = applicationProperties.getScript();

        ApplicationProperties.Script.RunnerLimits runnerLimits = script.getRunnerLimits();

        Map<String, String> properties = runnerProperties(script, runnerType);

        Duration maxWait = parseDuration(properties.get(MAX_WAIT_PROPERTY), runnerType, MAX_WAIT_PROPERTY);

        if (maxWait == null) {
            maxWait = runnerLimits.getMaxWait();
        }

        Long maxLogBytes = parseBytes(properties.get(MAX_LOG_BYTES_PROPERTY), runnerType, MAX_LOG_BYTES_PROPERTY);

        if (maxLogBytes == null) {
            DataSize dataSize = runnerLimits.getMaxLogBytes();

            maxLogBytes = dataSize == null ? null : dataSize.toBytes();
        }

        Long maxTransferBytes = parseBytes(
            properties.get(MAX_TRANSFER_BYTES_PROPERTY), runnerType, MAX_TRANSFER_BYTES_PROPERTY);

        if (maxTransferBytes == null) {
            DataSize dataSize = runnerLimits.getMaxTransferBytes();

            maxTransferBytes = dataSize == null ? null : dataSize.toBytes();
        }

        if (maxWait != null && (maxWait.isZero() || maxWait.isNegative())) {
            throw new IllegalArgumentException(
                "%s must be a positive duration, but is %s".formatted(
                    propertyKey(properties, runnerType, MAX_WAIT_PROPERTY), maxWait));
        }

        if (maxLogBytes != null && maxLogBytes < 2) {
            throw new IllegalArgumentException(
                "%s must be at least 2 bytes, but is %d".formatted(
                    propertyKey(properties, runnerType, MAX_LOG_BYTES_PROPERTY), maxLogBytes));
        }

        if (maxTransferBytes != null && maxTransferBytes <= 0) {
            throw new IllegalArgumentException(
                "%s must be a positive size, but is %d bytes".formatted(
                    propertyKey(properties, runnerType, MAX_TRANSFER_BYTES_PROPERTY), maxTransferBytes));
        }

        return new TaskRunnerLimits(
            maxWait == null ? DEFAULT_MAX_WAIT : maxWait,
            maxLogBytes == null ? DEFAULT_MAX_LOG_BYTES : maxLogBytes,
            maxTransferBytes == null ? DEFAULT_MAX_TRANSFER_BYTES : maxTransferBytes);
    }

    public long getMaxLogBytes() {
        return maxLogBytes;
    }

    public long getMaxTransferBytes() {
        return maxTransferBytes;
    }

    public Duration getMaxWait() {
        return maxWait;
    }

    @Nullable
    private static Long parseBytes(@Nullable String value, String runnerType, String propertyName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return DataSize.parse(value.trim())
                .toBytes();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Cannot read '%s' as a size; an operator must correct %s".formatted(
                    value, TaskRunnerOperatorFlag.getPropertyName(runnerType, propertyName)),
                exception);
        }
    }

    @Nullable
    private static Duration parseDuration(@Nullable String value, String runnerType, String propertyName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Duration.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "Cannot read '%s' as an ISO-8601 duration; an operator must correct %s".formatted(
                    value, TaskRunnerOperatorFlag.getPropertyName(runnerType, propertyName)),
                exception);
        }
    }

    private static String propertyKey(Map<String, String> properties, String runnerType, String propertyName) {
        String value = properties.get(propertyName);

        if (value == null || value.isBlank()) {
            return GLOBAL_PREFIX + propertyName;
        }

        return TaskRunnerOperatorFlag.getPropertyName(runnerType, propertyName);
    }

    private static Map<String, String> runnerProperties(ApplicationProperties.Script script, String runnerType) {
        Map<String, ApplicationProperties.Script.Runner> runners = script.getRunners();

        if (runners == null) {
            return Map.of();
        }

        ApplicationProperties.Script.Runner runner = runners.get(runnerType);

        if (runner == null || runner.getProperties() == null) {
            return Map.of();
        }

        return runner.getProperties();
    }
}
