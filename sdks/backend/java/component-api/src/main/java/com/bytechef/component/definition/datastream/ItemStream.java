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

package com.bytechef.component.definition.datastream;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * Defines the lifecycle callbacks shared by the readers and writers that participate in a data stream job. The
 * orchestrator invokes {@link #open} once before processing begins, {@link #update} periodically to persist restart
 * state into the {@link ExecutionContext}, and {@link #close} once when processing finishes.
 *
 * @author Ivica Cardic
 */
public interface ItemStream {

    /**
     * Opens the stream and prepares it for reading or writing. Implementations may initialize resources and restore any
     * previously stored restart state from the given {@link ExecutionContext}.
     *
     * @param inputParameters      the configured parameters for this element
     * @param connectionParameters the connection credentials, if applicable
     * @param context              the execution context of the surrounding component
     * @param executionContext     the stream execution context holding restartable state
     */
    default void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {
    }

    /**
     * Persists the current position or progress of the stream into the given {@link ExecutionContext} so that the job
     * can be restarted from this point if necessary.
     *
     * @param inputParameters      the configured parameters for this element
     * @param connectionParameters the connection credentials, if applicable
     * @param context              the execution context of the surrounding component
     * @param executionContext     the stream execution context into which restartable state is written
     */
    default void update(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {
    }

    /**
     * Closes the stream and releases any resources acquired in {@link #open}.
     */
    default void close() {
    }
}
