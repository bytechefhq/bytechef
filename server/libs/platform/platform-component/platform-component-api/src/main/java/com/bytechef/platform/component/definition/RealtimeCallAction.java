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

package com.bytechef.platform.component.definition;

/**
 * Marker interface for actions that execute a sub-workflow synchronously during real-time communication (e.g., voice
 * calls). Actions implementing this interface:
 *
 * <ul>
 * <li>Execute a sub-workflow synchronously during the action execution</li>
 * <li>Block until an external event (e.g., call completion) occurs</li>
 * <li>Need access to call session registry and job coordination</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public interface RealtimeCallAction {

    /**
     * Returns the property name that contains the sub-workflow ID to execute during the real-time call.
     *
     * @return the property name containing the sub-workflow ID
     */
    String getSubWorkflowIdProperty();
}
