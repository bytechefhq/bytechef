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

package com.bytechef.platform.component.runner.remote;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.time.Duration;
import java.util.Map;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * What a backend may read besides the request: the task's runner connection and the operator's properties for this
 * runner type, plus the daemon or endpoint the task named through its {@code host} property when the backend has one.
 * The context also carries the task's runner parameters (for calls that only receive a key or handle), the operator's
 * transfer ceiling, and the effective completion budget. Backends never read {@code ApplicationProperties} themselves.
 */
public record RemoteJobContext(
    @Nullable ComponentConnection connection, Map<String, String> operatorProperties, @Nullable String taskHost,
    Parameters runnerParameters, long maxTransferBytes, Duration completionBudget) {

    public RemoteJobContext {
        operatorProperties = Map.copyOf(operatorProperties);

        if (maxTransferBytes <= 0) {
            throw new IllegalArgumentException("maxTransferBytes must be greater than zero");
        }

        if (completionBudget.isZero() || completionBudget.isNegative()) {
            throw new IllegalArgumentException("completionBudget must be greater than zero");
        }
    }

    public boolean isOperatorFlagEnabled(String propertyName) {
        return Boolean.parseBoolean(operatorProperties.get(propertyName));
    }

    /**
     * Names the connection and lists only the keys of the operator properties and runner parameters, so credentials
     * never reach a log line.
     */
    @Override
    public String toString() {
        String connectionText = connection == null
            ? "none" : connection.getComponentName() + "/v" + connection.getVersion();

        return ("RemoteJobContext[connection=%s, operatorProperties=%s, taskHost=%s, runnerParameters=%s, " +
            "maxTransferBytes=%d, completionBudget=%s]").formatted(
                connectionText, new TreeSet<>(operatorProperties.keySet()), taskHost,
                new TreeSet<>(runnerParameters.keySet()), maxTransferBytes, completionBudget);
    }
}
