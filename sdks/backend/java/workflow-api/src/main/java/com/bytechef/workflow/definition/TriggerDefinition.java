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

package com.bytechef.workflow.definition;

import java.util.Map;

/**
 * Defines a trigger that can initiate the execution of a {@link WorkflowDefinition}.
 *
 * <p>
 * A trigger binds a workflow to a specific event source — a webhook, a schedule, a polling component operation —
 * identified by its {@link #getType() type}, and carries the parameters that configure how that event source behaves.
 *
 * <p>
 * The trigger itself is not code: it names a component trigger the platform already provides, so a code workflow starts
 * the same way a visually built one does.
 *
 * @author Ivica Cardic
 */
public interface TriggerDefinition {

    /**
     * Returns the type of the trigger, naming the component and trigger operation that provides it — for example
     * {@code "schedule/v1/interval"} or {@code "workflow/v1/newWorkflowCall"}.
     *
     * @return the trigger type
     */
    String getType();

    /**
     * Returns the name that uniquely identifies this trigger within its workflow.
     *
     * @return the trigger name
     */
    String getName();

    /**
     * Returns the parameters that configure the trigger, keyed by parameter name — the same values a visual workflow
     * sets on the trigger node.
     *
     * @return the trigger's parameters
     */
    Map<String, ?> getParameters();
}
