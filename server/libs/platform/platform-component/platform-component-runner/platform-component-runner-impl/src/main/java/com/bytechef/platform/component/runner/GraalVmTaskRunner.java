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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.GRAALVM;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.MODE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.STRICT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.TRUSTED;

import com.bytechef.component.definition.ComponentDsl.ModifiableOption;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.polyglot.ScriptSandboxMode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class GraalVmTaskRunner implements TaskRunner {

    private static final Duration DEFAULT_TRUSTED_TIMEOUT = Duration.ofMinutes(5);

    private final PolyglotEngine polyglotEngine;
    private final ApplicationProperties applicationProperties;

    public GraalVmTaskRunner(PolyglotEngine polyglotEngine, ApplicationProperties applicationProperties) {
        this.polyglotEngine = polyglotEngine;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getType() {
        return GRAALVM;
    }

    @Override
    public String getTitle() {
        return "GraalVM";
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties() {
        boolean trustedEnabled = TaskRunnerTrustedMode.isEnabled(applicationProperties, GRAALVM);

        List<ModifiableOption<String>> options = new ArrayList<>();

        options.add(option("Strict", STRICT));

        if (trustedEnabled) {
            options.add(option("Trusted", TRUSTED));
        }

        String description = trustedEnabled
            ? "Strict runs the script with no access to the host: no file system, no environment, no process " +
                "creation, and CPU and memory ceilings. Trusted lifts every restriction and runs the script inside " +
                "the server process with full reflection; use it only on a single-tenant deployment you control."
            : "Strict runs the script with no access to the host: no file system, no environment, no process " +
                "creation, and CPU and memory ceilings.";

        return List.of(
            string(MODE)
                .label("Mode")
                .description(description)
                .options(options)
                .defaultValue(STRICT)
                .required(false));
    }

    @Override
    public Set<TaskRunnerCapability> getCapabilities() {
        return Set.of(TaskRunnerCapability.INLINE_SCRIPT, TaskRunnerCapability.COMPONENT_BRIDGE);
    }

    @Override
    public void validate(TaskRunnerRequest request) {
        if (!request.commands()
            .isEmpty()) {

            throw new IllegalArgumentException("The GraalVM runner does not support commands; use a script instead");
        }

        if (!request.inputFiles()
            .isEmpty()) {

            throw new IllegalArgumentException("The GraalVM runner does not support inputFiles");
        }

        if (!request.outputFilePatterns()
            .isEmpty()) {

            throw new IllegalArgumentException("The GraalVM runner does not support outputFiles");
        }

        if (!request.env()
            .isEmpty()) {

            throw new IllegalArgumentException("The GraalVM runner does not support env");
        }

        ScriptSandboxMode scriptSandboxMode = getMode(request);

        if (scriptSandboxMode == ScriptSandboxMode.STRICT && request.timeout() != null) {
            throw new IllegalArgumentException(
                "The GraalVM runner's strict mode does not support timeout; strict already runs under CPU and " +
                    "heap ceilings, and a wall clock would kill a script that is merely waiting on I/O, such as a " +
                    "slow call through the component bridge");
        }
    }

    @Override
    public TaskRunnerResult run(TaskRunnerRequest request) {
        validate(request);

        ScriptSandboxMode scriptSandboxMode = getMode(request);

        Object output = polyglotEngine.execute(
            scriptSandboxMode, getTimeout(request, scriptSandboxMode), request.languageId(),
            request.inputParameters(), request.componentConnections(), (JobContextAware) request.actionContext());

        return TaskRunnerResult.ofOutput(output);
    }

    private static @Nullable Duration getTimeout(TaskRunnerRequest request, ScriptSandboxMode scriptSandboxMode) {
        Duration timeout = request.timeout();

        if (timeout != null) {
            return timeout;
        }

        return scriptSandboxMode == ScriptSandboxMode.TRUSTED ? DEFAULT_TRUSTED_TIMEOUT : null;
    }

    private ScriptSandboxMode getMode(TaskRunnerRequest request) {
        String mode = request.runnerParameters()
            .getString(MODE, STRICT);

        ScriptSandboxMode scriptSandboxMode = switch (mode) {
            case STRICT -> ScriptSandboxMode.STRICT;
            case TRUSTED -> ScriptSandboxMode.TRUSTED;
            default -> throw new IllegalArgumentException(
                "Unknown GraalVM sandbox mode '%s'; expected '%s' or '%s'".formatted(mode, STRICT, TRUSTED));
        };

        if (scriptSandboxMode == ScriptSandboxMode.TRUSTED &&
            !TaskRunnerTrustedMode.isEnabled(applicationProperties, GRAALVM)) {

            throw new IllegalArgumentException(
                "The GraalVM runner's '%s' mode is not enabled. An operator must enable it with %s=true."
                    .formatted(TRUSTED, TaskRunnerTrustedMode.getPropertyName(GRAALVM)));
        }

        return scriptSandboxMode;
    }
}
