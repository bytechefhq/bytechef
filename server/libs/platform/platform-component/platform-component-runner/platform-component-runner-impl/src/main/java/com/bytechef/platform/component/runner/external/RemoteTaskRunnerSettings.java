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

package com.bytechef.platform.component.runner.external;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.DELETE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PULL_POLICY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.RESUME;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.WAIT_UNTIL_COMPLETION;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.WAIT_UNTIL_RUNNING;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.runner.TaskRunnerLimits;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.jspecify.annotations.Nullable;

record RemoteTaskRunnerSettings(
    boolean resume, boolean delete, Duration waitUntilRunning, Duration waitUntilCompletion,
    RemotePullPolicy pullPolicy) {

    private static final Duration DEFAULT_WAIT_UNTIL_RUNNING = Duration.ofMinutes(10);
    private static final Duration DEFAULT_WAIT_UNTIL_COMPLETION = Duration.ofHours(1);

    static RemoteTaskRunnerSettings of(TaskRunnerRequest request, TaskRunnerLimits limits, String type) {
        Parameters parameters = request.runnerParameters();

        Duration maxWait = limits.getMaxWait();

        Duration requestTimeout = request.timeout();

        Duration waitUntilCompletion = requestTimeout != null
            ? requestTimeout
            : parseDuration(parameters.getString(WAIT_UNTIL_COMPLETION), WAIT_UNTIL_COMPLETION, type,
                DEFAULT_WAIT_UNTIL_COMPLETION);

        if (waitUntilCompletion.compareTo(maxWait) > 0) {
            waitUntilCompletion = maxWait;
        }

        Duration waitUntilRunning = parseDuration(
            parameters.getString(WAIT_UNTIL_RUNNING), WAIT_UNTIL_RUNNING, type, DEFAULT_WAIT_UNTIL_RUNNING);

        if (waitUntilRunning.compareTo(waitUntilCompletion) > 0) {
            waitUntilRunning = waitUntilCompletion;
        }

        String pullPolicy = parameters.getString(PULL_POLICY);

        return new RemoteTaskRunnerSettings(
            parameters.getBoolean(RESUME, true), parameters.getBoolean(DELETE, true), waitUntilRunning,
            waitUntilCompletion,
            pullPolicy == null || pullPolicy.isBlank() ? RemotePullPolicy.IF_NOT_PRESENT
                : parsePullPolicy(pullPolicy, type));
    }

    static List<ModifiableValueProperty<?, ?>> properties() {
        return List.of(
            string(PULL_POLICY)
                .label("Pull Policy")
                .description("When the image is pulled from its registry.")
                .options(
                    option("Always", RemotePullPolicy.ALWAYS.name()),
                    option("If Not Present", RemotePullPolicy.IF_NOT_PRESENT.name()),
                    option("Never", RemotePullPolicy.NEVER.name()))
                .defaultValue(RemotePullPolicy.IF_NOT_PRESENT.name())
                .required(false),
            bool(RESUME)
                .label("Resume")
                .description(
                    "After a server restart, re-attach to the job this task execution already started instead of " +
                        "starting another one.")
                .defaultValue(true)
                .required(false),
            bool(DELETE)
                .label("Delete")
                .description(
                    "Remove the job and whatever it left behind once it has finished. Turn it off to inspect a " +
                        "failed job by hand.")
                .defaultValue(true)
                .required(false),
            string(WAIT_UNTIL_RUNNING)
                .label("Wait Until Running")
                .description(
                    "How long the job may take to be scheduled, pull its image and start, as an ISO-8601 duration.")
                .defaultValue("PT10M")
                .required(false),
            string(WAIT_UNTIL_COMPLETION)
                .label("Wait Until Completion")
                .description(
                    "How long the job may run before it is killed, as an ISO-8601 duration. The task's Timeout, " +
                        "when set, takes precedence; both are capped by the operator's max-wait.")
                .defaultValue("PT1H")
                .required(false));
    }

    private static Duration parseDuration(
        @Nullable String value, String propertyName, String type, Duration defaultValue) {

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        Duration duration;

        try {
            duration = Duration.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The %s runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    type, value, propertyName),
                exception);
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                "The %s runner's %s must be greater than zero".formatted(type, propertyName));
        }

        return duration;
    }

    private static RemotePullPolicy parsePullPolicy(String value, String type) {
        try {
            return RemotePullPolicy.of(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "The %s runner does not know the pull policy '%s'".formatted(type, value), exception);
        }
    }
}
