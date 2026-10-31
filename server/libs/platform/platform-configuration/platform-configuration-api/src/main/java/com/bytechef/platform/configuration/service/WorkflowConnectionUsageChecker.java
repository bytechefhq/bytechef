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

package com.bytechef.platform.configuration.service;

/**
 * Checks that the current caller may bind a connection to a workflow in an environment.
 *
 * @author Ivica Cardic
 */
public interface WorkflowConnectionUsageChecker {

    /**
     * Throws when the current caller may not bind the connection to the workflow in the environment.
     */
    void checkConnectionUsage(String workflowId, long connectionId, long environmentId);
}
