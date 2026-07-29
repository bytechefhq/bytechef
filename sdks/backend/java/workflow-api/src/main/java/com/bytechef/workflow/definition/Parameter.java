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
 * Marker interface for a single configuration parameter supplied to a workflow task or trigger.
 *
 * <p>
 * Implementations carry the concrete key/value binding that configures a {@link TaskDefinition} or
 * {@link TriggerDefinition}. The interface is intentionally empty so that different definition backends can provide
 * their own parameter representations while still being exposed uniformly through the workflow definition API.
 *
 * @author Ivica Cardic
 */
public interface Parameter {

}
