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
 * Marker interface for an input declared by a {@link WorkflowDefinition}.
 *
 * <p>
 * An input describes a value that a workflow expects to receive when it is executed, such as a user-provided argument
 * or a value fed in by a trigger. The interface is intentionally empty so that concrete definition backends can supply
 * their own input representations while exposing them uniformly through the workflow definition API.
 *
 * @author Ivica Cardic
 */
public interface Input {
}
