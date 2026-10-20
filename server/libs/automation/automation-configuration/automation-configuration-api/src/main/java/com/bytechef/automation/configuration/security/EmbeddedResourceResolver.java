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

package com.bytechef.automation.configuration.security;

/**
 * Tells the workspace permission checks whether a project or workflow belongs to the embedded side, so that no
 * workspace role governs it. An integration workflow and a Hub project workflow are answered by embedded scopes; a
 * connected-user project workflow is denied until its embedded gates exist.
 *
 * @author Ivica Cardic
 */
public interface EmbeddedResourceResolver {

    boolean isEmbeddedProject(long projectId);

    boolean isEmbeddedProjectWorkflow(String workflowId);

    boolean isHubProject(long projectId);

    boolean isHubProjectWorkflow(String workflowId);

    boolean isIntegrationWorkflow(String workflowId);
}
