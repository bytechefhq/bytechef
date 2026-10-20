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
import java.util.ArrayList;
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
        givenConfiguration(20L, 1L, Environment.STAGING);
        givenIntegrationWorkflows(1L, WORKFLOW_ID);
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).contains(Environment.STAGING);
        assertThat(ownershipResolver.resolveOwner(JOB_ID)).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAConfigurationTestJobTakesTheConfigurationsEnvironment() {
        givenConfiguration(20L, 1L, Environment.DEVELOPMENT);
        givenIntegrationWorkflows(1L, WORKFLOW_ID);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(20L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).contains(Environment.DEVELOPMENT);
    }

    @Test
    void testAnOldRunStillTakesItsInstanceEnvironmentAfterAnUpgrade() {
        givenConfiguration(20L, 1L, Environment.PRODUCTION);
        givenConfiguration(10L, 2L, Environment.DEVELOPMENT);
        givenIntegrationWorkflows(1L, WORKFLOW_ID, "wf-1-v2");
        givenIntegrationWorkflows(2L, "unrelated");
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).contains(Environment.PRODUCTION);
    }

    @Test
    void testACollidingConfigurationOfTheSameIntegrationMakesTheEnvironmentEmpty() {
        givenConfiguration(20L, 1L, Environment.STAGING);
        givenConfiguration(10L, 1L, Environment.PRODUCTION);
        givenIntegrationWorkflows(1L, WORKFLOW_ID);
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
    }

    @Test
    void testBothMatchingWithTheSameEnvironmentGiveThatEnvironment() {
        givenConfiguration(20L, 1L, Environment.STAGING);
        givenConfiguration(10L, 1L, Environment.STAGING);
        givenIntegrationWorkflows(1L, WORKFLOW_ID);
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).contains(Environment.STAGING);
    }

    @Test
    void testAJobWhoseWorkflowIsOutsideTheIntegrationHasNoEnvironment() {
        givenConfiguration(20L, 1L, Environment.STAGING);
        givenIntegrationWorkflows(1L, "other");
        givenInstance(10L, 20L);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(10L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
    }

    @Test
    void testAConfigurationWithoutAnIntegrationHasNoEnvironment() {
        givenConfiguration(20L, null, Environment.STAGING);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED)).thenReturn(Optional.of(20L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
    }

    @Test
    void testAnAutomationJobIsUnknown() {
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION)).thenReturn(Optional.of(5L));

        assertThat(environmentResolver.fetchEnvironment(JOB_ID)).isEmpty();
        assertThat(ownershipResolver.resolveOwner(JOB_ID)).isEqualTo(ResourceOwner.unknown());
    }

    private void givenConfiguration(long id, Long integrationId, Environment environment) {
        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(environment);
        configuration.setId(id);
        configuration.setIntegrationId(integrationId);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(id)))
            .thenReturn(List.of(configuration));
    }

    private void givenIntegrationWorkflows(long integrationId, String... workflowIds) {
        List<IntegrationWorkflow> integrationWorkflows = new ArrayList<>();

        for (String workflowId : workflowIds) {
            integrationWorkflows.add(new IntegrationWorkflow(integrationId, 1, workflowId, UUID.randomUUID()));
        }

        when(integrationWorkflowService.getIntegrationWorkflows(integrationId)).thenReturn(integrationWorkflows);
    }

    private void givenInstance(long id, long configurationId) {
        IntegrationInstance instance = new IntegrationInstance();

        instance.setId(id);
        instance.setIntegrationInstanceConfigurationId(configurationId);

        when(integrationInstanceService.getIntegrationInstances(List.of(id))).thenReturn(List.of(instance));
    }
}
