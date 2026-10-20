/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedJobResolversTest {

    private static final long JOB_ID = 7L;
    private static final String WORKFLOW_ID = "wf-1";

    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private IntegrationInstanceService integrationInstanceService;
    private IntegrationWorkflowService integrationWorkflowService;
    private JobService jobService;
    private PrincipalJobService principalJobService;
    private EmbeddedJobEnvironmentResolver environmentResolver;
    private EmbeddedJobOwnershipResolver ownershipResolver;

    @BeforeEach
    void setUp() {
        integrationInstanceConfigurationService = mock(IntegrationInstanceConfigurationService.class);
        integrationInstanceService = mock(IntegrationInstanceService.class);
        integrationWorkflowService = mock(IntegrationWorkflowService.class);
        jobService = mock(JobService.class);
        principalJobService = mock(PrincipalJobService.class);

        EmbeddedPrincipalEnvironmentResolver principalEnvironmentResolver = new EmbeddedPrincipalEnvironmentResolver(
            integrationInstanceConfigurationService, integrationInstanceService, integrationWorkflowService);

        environmentResolver = new EmbeddedJobEnvironmentResolver(
            principalEnvironmentResolver, jobService, principalJobService);
        ownershipResolver = new EmbeddedJobOwnershipResolver(principalJobService);

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.empty());
    }

    @Test
    void testARuntimeJobTakesItsInstanceConfigurationsEnvironment() {
        givenConfiguration(20L, 1L, 3, Environment.STAGING, true);
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).contains(Environment.STAGING);
        assertThat(ownershipResolver.resolveOwner(JOB_ID)).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAConfigurationTestJobTakesTheConfigurationsEnvironment() {
        givenConfiguration(20L, 1L, 3, Environment.DEVELOPMENT, true);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(20L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).contains(Environment.DEVELOPMENT);
    }

    @Test
    void testAnAmbiguousPrincipalHasNoEnvironment() {
        givenConfiguration(20L, 1L, 3, Environment.STAGING, true);
        givenConfiguration(10L, 1L, 3, Environment.PRODUCTION, true);
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
    }

    @Test
    void testAJobWhoseWorkflowIsOutsideTheIntegrationVersionHasNoEnvironment() {
        givenConfiguration(20L, 1L, 3, Environment.STAGING, false);
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
    }

    @Test
    void testAnAutomationJobIsUnknown() {
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
        assertThat(ownershipResolver.resolveOwner(JOB_ID)).isEqualTo(ResourceOwner.unknown());
    }

    private void givenConfiguration(
        long id, long integrationId, int integrationVersion, Environment environment, boolean containsWorkflow) {

        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(environment);
        configuration.setId(id);
        configuration.setIntegrationId(integrationId);
        configuration.setIntegrationVersion(integrationVersion);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(id)))
            .thenReturn(List.of(configuration));

        String workflowId = containsWorkflow ? WORKFLOW_ID : "other";

        when(integrationWorkflowService.getIntegrationWorkflows(integrationId, integrationVersion))
            .thenReturn(
                List.of(new IntegrationWorkflow(integrationId, integrationVersion, workflowId, UUID.randomUUID())));
    }

    private void givenInstance(long id, long configurationId) {
        IntegrationInstance instance = new IntegrationInstance();

        instance.setId(id);
        instance.setIntegrationInstanceConfigurationId(configurationId);

        when(integrationInstanceService.getIntegrationInstances(List.of(id))).thenReturn(List.of(instance));
    }
}
