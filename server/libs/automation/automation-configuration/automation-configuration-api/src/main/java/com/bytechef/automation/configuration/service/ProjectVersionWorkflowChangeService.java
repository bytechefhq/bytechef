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

package com.bytechef.automation.configuration.service;

import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;

/**
 * Computes workflow changes between a project version and the previous existing project version. Not guarded; callers
 * must be guarded facades.
 *
 * @author Ivica Cardic
 */
public interface ProjectVersionWorkflowChangeService {

    VersionWorkflowChanges getVersionWorkflowChanges(long projectId, int projectVersion);

    VersionWorkflowChange getVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid);
}
