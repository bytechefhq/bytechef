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

package com.bytechef.platform.job.sync.simulation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Outcome of a workflow dry-run simulation.
 *
 * <p>
 * When {@link Outcome#COMPLETED}, the failure-describing fields are {@code null}. When {@link Outcome#FAILED},
 * {@code failedTaskName} and {@code failedTaskType} describe the first failed task (both {@code null} when the engine
 * did not attribute the failure to a specific task, e.g. a timeout or cancel); {@code reason} is always populated,
 * falling back to a generic status-bearing message when no task- or job-level error is available.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record WorkflowSimulationResult(
    Outcome outcome, @Nullable String failedTaskName, @Nullable String failedTaskType, @Nullable String reason,
    List<String> warnings) {

    public enum Outcome {
        COMPLETED, FAILED
    }
}
