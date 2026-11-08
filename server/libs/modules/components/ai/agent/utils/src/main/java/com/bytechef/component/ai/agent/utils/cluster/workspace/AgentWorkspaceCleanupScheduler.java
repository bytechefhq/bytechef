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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.service.TenantService;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AgentWorkspaceCleanupScheduler {

    private static final Duration DEFAULT_TEST_RETENTION = Duration.ofHours(24);

    private final ApplicationProperties.Ai.AgentUtils agentUtils;
    private final ObjectProvider<JobPrincipalAccessorRegistry> jobPrincipalAccessorRegistryProvider;
    private final ObjectProvider<TenantService> tenantServiceProvider;

    public AgentWorkspaceCleanupScheduler(
        ApplicationProperties applicationProperties,
        ObjectProvider<JobPrincipalAccessorRegistry> jobPrincipalAccessorRegistryProvider,
        ObjectProvider<TenantService> tenantServiceProvider) {

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        this.agentUtils = ai.getAgentUtils();
        this.jobPrincipalAccessorRegistryProvider = jobPrincipalAccessorRegistryProvider;
        this.tenantServiceProvider = tenantServiceProvider;
    }

    @Scheduled(
        initialDelayString = "PT5M",
        fixedDelayString = "${bytechef.ai.agent-utils.workspace.cleanup-interval:PT1H}")
    public void cleanAgentWorkspaces() {
        ApplicationProperties.Ai.AgentUtils.FileSystem fileSystem = agentUtils.getFileSystem();
        ApplicationProperties.Ai.AgentUtils.Shell shell = agentUtils.getShell();

        if (!fileSystem.isEnabled() && !shell.isEnabled()) {
            return;
        }

        ApplicationProperties.Ai.AgentUtils.Workspace workspace = agentUtils.getWorkspace();

        String basedir = workspace.getBasedir();

        if (basedir == null || basedir.isBlank()) {
            return;
        }

        Duration testRetention = workspace.getTestRetention();

        AgentWorkspaceCleaner agentWorkspaceCleaner = new AgentWorkspaceCleaner(
            Path.of(basedir), testRetention == null ? DEFAULT_TEST_RETENTION : testRetention,
            workspace.getIdleRetention(), tenantServiceProvider.getIfAvailable(),
            jobPrincipalAccessorRegistryProvider.getIfAvailable());

        agentWorkspaceCleaner.clean();
    }
}
