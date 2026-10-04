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

package com.bytechef.automation.ai.a2a.facade;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface A2aProjectFacade {

    A2aProject createA2aProject(long a2aServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds);

    void deleteA2aProject(long a2aProjectId);

    A2aProject updateA2aProject(long a2aProjectId, List<String> selectedWorkflowIds);
}
