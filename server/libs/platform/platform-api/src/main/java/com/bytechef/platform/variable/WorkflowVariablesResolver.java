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

package com.bytechef.platform.variable;

import com.bytechef.platform.constant.PlatformType;
import java.util.Map;

/**
 * Resolves the variables (name to string value) visible to a workflow run. Optional CE seam: implemented by the EE
 * {@code platform-variable} module; when no bean is present, CE consumers do not seed a {@code vars} job input at all.
 * Implementations MUST fail open (return an empty map, never throw) — a variable-store outage must not stop jobs.
 *
 * @author Ivica Cardic
 */
public interface WorkflowVariablesResolver {

    /**
     * Variables for a job created for the given principal (automation project deployment / embedded integration
     * instance), in that principal's environment.
     */
    Map<String, String> resolveForJobPrincipal(long jobPrincipalId, PlatformType type);

    /**
     * Variables for editor previews and test runs of the given workflow in the given environment.
     */
    Map<String, String> resolveForWorkflow(String workflowId, long environmentId);
}
