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

package com.bytechef.automation.configuration.event;

import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.billing.event.TrialExpiredEvent;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class TrialExpiredEventListener {

    private static final Logger log = LoggerFactory.getLogger(TrialExpiredEventListener.class);

    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public TrialExpiredEventListener(ProjectDeploymentService projectDeploymentService) {
        this.projectDeploymentService = projectDeploymentService;
    }

    @EventListener
    public void onTrialExpired(TrialExpiredEvent event) {
        log.info("Trial expired — disabling all project deployments for tenant {}", event.getTenantId());

        TenantContext.runWithTenantId(event.getTenantId(), projectDeploymentService::disableAllProjectDeployments);
    }
}
