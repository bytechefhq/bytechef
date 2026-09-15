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

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowConnectionUsageChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Checks a connection binding through {@link PermissionService#canUseConnectionInWorkflow}.
 *
 * @author Ivica Cardic
 */
@Component
public class AutomationWorkflowConnectionUsageChecker implements WorkflowConnectionUsageChecker {

    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowConnectionUsageChecker(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void checkConnectionUsage(String workflowId, long connectionId, long environmentId) {
        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length ||
            !permissionService.canUseConnectionInWorkflow(
                connectionId, workflowId, environments[(int) environmentId])) {

            throw new AccessDeniedException(
                "Connection id=%s cannot be used by workflow id=%s".formatted(connectionId, workflowId));
        }
    }
}
