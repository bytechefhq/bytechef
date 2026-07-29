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

import java.util.Map;

/**
 * Runs a workflow as a dry-run (no real component side effects) and reports whether the DAG would complete or which
 * task would fail.
 *
 * @author Ivica Cardic
 */
public interface WorkflowSimulationFacade {

    /**
     * Simulates the execution of the given workflow with the supplied inputs. Component actions and triggers return
     * their declared sample output instead of calling out to real systems, so this can be used to validate a workflow's
     * wiring without producing side effects.
     *
     * @param workflowId the id of the workflow to simulate
     * @param inputs     the workflow inputs
     * @return the simulation outcome, including the first failing task when the run does not complete
     */
    WorkflowSimulationResult simulate(String workflowId, Map<String, ?> inputs);
}
