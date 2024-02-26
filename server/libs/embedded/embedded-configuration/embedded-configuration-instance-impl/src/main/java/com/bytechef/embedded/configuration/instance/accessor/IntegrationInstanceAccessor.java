/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.instance.accessor;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.constant.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class IntegrationInstanceAccessor implements InstanceAccessor {

    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceAccessor(
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService) {

        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return integrationInstanceWorkflowService.isConnectionUsed(connectionId);
    }

    @Override
    public boolean isWorkflowEnabled(long instanceId, String workflowId) {
        boolean workflowEnabled = false;

        if (integrationInstanceService.isIntegrationInstanceEnabled(instanceId) &&
            integrationInstanceWorkflowService.isIntegrationInstanceWorkflowEnabled(instanceId, workflowId)) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public Map<String, ?> getInputMap(long instanceId, String workflowId) {
        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(instanceId, workflowId);

        return integrationInstanceWorkflow.getInputs();
    }

    @Override
    public Type getType() {
        return Type.EMBEDDED;
    }
}
