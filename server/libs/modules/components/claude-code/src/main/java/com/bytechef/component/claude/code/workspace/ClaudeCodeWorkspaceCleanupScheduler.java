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

package com.bytechef.component.claude.code.workspace;

import com.bytechef.component.claude.code.util.ClaudeCodeUtil;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.workspace.AgentWorkspaceCleaner;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.service.TenantService;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.component.claude-code", name = "enabled", havingValue = "true")
public class ClaudeCodeWorkspaceCleanupScheduler {

    private static final Duration DEFAULT_TEST_RETENTION = Duration.ofHours(24);

    private final ApplicationProperties.Component.ClaudeCode claudeCode;
    private final ObjectProvider<JobPrincipalAccessorRegistry> jobPrincipalAccessorRegistryProvider;
    private final ObjectProvider<TenantService> tenantServiceProvider;

    public ClaudeCodeWorkspaceCleanupScheduler(
        ApplicationProperties applicationProperties,
        ObjectProvider<JobPrincipalAccessorRegistry> jobPrincipalAccessorRegistryProvider,
        ObjectProvider<TenantService> tenantServiceProvider) {

        ApplicationProperties.Component component = applicationProperties.getComponent();

        this.claudeCode = component.getClaudeCode();
        this.jobPrincipalAccessorRegistryProvider = jobPrincipalAccessorRegistryProvider;
        this.tenantServiceProvider = tenantServiceProvider;
    }

    @Scheduled(
        initialDelayString = "PT5M",
        fixedDelayString = "${bytechef.component.claude-code.workspace.cleanup-interval:PT1H}")
    public void cleanClaudeCodeWorkspaces() {
        ApplicationProperties.Component.ClaudeCode.Workspace workspace = claudeCode.getWorkspace();

        String basedir = workspace.getBasedir();

        if (basedir == null || basedir.isBlank()) {
            return;
        }

        Duration testRetention = workspace.getTestRetention();

        AgentWorkspaceCleaner agentWorkspaceCleaner = new AgentWorkspaceCleaner(
            Path.of(basedir), testRetention == null ? DEFAULT_TEST_RETENTION : testRetention,
            workspace.getIdleRetention(), tenantServiceProvider.getIfAvailable(),
            jobPrincipalAccessorRegistryProvider.getIfAvailable(), ClaudeCodeUtil::isWorkspaceInUse);

        agentWorkspaceCleaner.clean();
    }
}
