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

import java.util.List;

/**
 * Defines a trigger that can initiate the execution of a {@link WorkflowDefinition}.
 *
 * <p>
 * A trigger binds a workflow to a specific event source (for example a webhook, a schedule, or a polling component
 * operation), identified by its {@link #getType() type}, and carries the {@link Parameter parameters} that configure
 * how that event source behaves.
 *
 * @author Ivica Cardic
 */
public interface TriggerDefinition {

    /**
     * Returns the type of the trigger, typically referencing the component and trigger operation that provides it (for
     * example {@code "webhook/newWebhook"}).
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
     * Returns the parameters that configure the behavior of this trigger.
     *
     * @return the list of configuration parameters for the trigger
     */
    List<? extends Parameter> getParameters();
}
