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

/**
 * An output a {@link WorkflowDefinition} produces once it completes.
 *
 * <p>
 * The value is evaluated against the job context at completion and becomes the job's outputs — the body a synchronous
 * caller receives, and what {@code GET /workflow-executions/{id}} reports for an asynchronous run.
 *
 * @author Ivica Cardic
 */
public interface Output {

    /**
     * Returns the name the value is reported under.
     */
    String getName();

    /**
     * Returns the name of the task whose output is the value, or {@code null} when {@link #getValue()} carries an
     * expression instead. Naming the task is the only way to reach one whose name is not a plain identifier, since a
     * {@code ${...}} reference cannot express a hyphen.
     */
    String getTask();

    /**
     * Returns the value, either a literal or a {@code ${...}} expression evaluated against the job context. Null when
     * {@link #getTask()} names a task instead.
     */
    Object getValue();
}
