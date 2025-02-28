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

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.constant.ModeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class IntegrationJobPrincipalAccessor implements JobPrincipalAccessor {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationJobPrincipalAccessor(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationWorkflowService integrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return integrationInstanceConfigurationWorkflowService.isConnectionUsed(connectionId);
    }

    @Override
    public boolean isWorkflowEnabled(long principalId, String workflowReferenceCode) {
        boolean workflowEnabled = false;

        if (integrationInstanceConfigurationService.isIntegrationInstanceConfigurationEnabled(principalId) &&
            integrationInstanceConfigurationWorkflowService.isIntegrationInstanceWorkflowEnabled(
                principalId, getWorkflowId(principalId, workflowReferenceCode))) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public Map<String, ?> getInputMap(long principalId, String workflowReferenceCode) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                principalId, getWorkflowId(principalId, workflowReferenceCode));

        return integrationInstanceConfigurationWorkflow.getInputs();
    }

    @Override
    public ModeType getType() {
        return ModeType.EMBEDDED;
    }

    @Override
    public String getWorkflowId(long principalId, String workflowReferenceCode) {
        return integrationWorkflowService.getWorkflowId(
            principalId, workflowReferenceCode);
    }

    @Override
    public String getLatestWorkflowId(String workflowReferenceCode) {
        return integrationWorkflowService.getLatestWorkflowId(workflowReferenceCode);
    }

    @Override
    public String getWorkflowReferenceCode(String workflowId) {
        IntegrationWorkflow workflowIntegrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            workflowId);

        return workflowIntegrationWorkflow.getWorkflowReferenceCode();
    }
}
