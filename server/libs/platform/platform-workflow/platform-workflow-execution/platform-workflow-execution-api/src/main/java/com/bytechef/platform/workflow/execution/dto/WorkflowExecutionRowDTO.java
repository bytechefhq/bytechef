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

package com.bytechef.platform.workflow.execution.dto;

/**
 * One row of the executions list before it is hydrated: either a job or a trigger execution that produced no job, in
 * the order the page query returned them.
 *
 * @author Ivica Cardic
 */
public record WorkflowExecutionRowDTO(Kind kind, long id) {

    public enum Kind {
        JOB,
        TRIGGER_EXECUTION
    }
}
