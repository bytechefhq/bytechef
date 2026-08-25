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

package com.bytechef.platform.workflow.execution.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanOveragePolicy;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.plan.provider.PlanOveragePolicyProvider;
import com.bytechef.platform.plan.provider.PlanSpendProvider;
import com.bytechef.platform.variable.WorkflowVariablesResolver;
import com.bytechef.platform.workflow.JobInputConstants;
import com.bytechef.platform.workflow.execution.exception.JobCostLimitExceededException;
import com.bytechef.platform.workflow.execution.service.LicenceJobUsageService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Tests {@link PrincipalJobFacadeImpl#createPrincipalLinkedJob} -- the new method added for the agent-tool sub-workflow
 * bridge. Covers both branches (principal present, principal missing) -- the missing-principal branch MUST throw rather
 * than silently create an orphan job (review finding C3).
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class PrincipalJobFacadeImplTest {

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private LicenceJobUsageService licenceJobUsageService;

    @Mock
    private WorkflowVariablesResolver workflowVariablesResolver;

    @Test
    void testCreatePrincipalLinkedJobCreatesJobAndLinksPrincipal() {
        long referenceJobId = 100L;
        long principalId = 7L;
        long newJobId = 200L;

        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        when(principalJobService.fetchJobPrincipalId(referenceJobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(principalId));
        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(newJobId);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider());

        long result = facade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION);

        assertEquals(newJobId, result);
        verify(principalJobService).create(newJobId, principalId, PlatformType.AUTOMATION);
    }

    /**
     * C3 regression: if the reference job has no principal, the facade must throw rather than silently create a job
     * with no tenant/billing attribution. The old behavior was log-and-continue, which produced orphan jobs invisible
     * to workspace-scoped lookups -- a silent multi-tenant data leak.
     */
    @Test
    void testCreatePrincipalLinkedJobThrowsWhenPrincipalMissing() {
        long referenceJobId = 100L;

        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        when(principalJobService.fetchJobPrincipalId(referenceJobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.empty());

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> facade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION));

        // No job must have been created -- C3 requires fail-fast, not create-then-orphan.

        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
        verify(principalJobService, never()).create(anyLong(), anyLong(), any());

        // Sanity-check the exception message gives operators what they need to diagnose.

        assertEquals(true, exception.getMessage()
            .contains(String.valueOf(referenceJobId)));
    }

    @Test
    void testCreateJobRejectedWhenMonthlyCostCapReached() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, new BigDecimal("10.00"), null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null,
            null, null, null, null, null);

        PlanLimitsProvider planLimitsProvider = tenantId -> planLimits;
        PlanSpendProvider planSpendProvider = tenantId -> new BigDecimal("10.00");

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(planLimitsProvider),
            emptyObjectProvider(), objectProviderOf(planSpendProvider), emptyObjectProvider(),
            emptyObjectProvider());

        assertThrows(
            JobCostLimitExceededException.class,
            () -> facade.createJob(jobParametersDTO, 1L, PlatformType.AUTOMATION));

        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
    }

    @Test
    void testCreateJobAdmittedWhenSpendBelowMonthlyCostCap() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, new BigDecimal("10.00"), null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null,
            null, null, null, null, null);

        PlanLimitsProvider planLimitsProvider = tenantId -> planLimits;
        PlanSpendProvider planSpendProvider = tenantId -> new BigDecimal("9.99");

        when(jobFacade.createJob(jobParametersDTO)).thenReturn(300L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(planLimitsProvider),
            emptyObjectProvider(), objectProviderOf(planSpendProvider), emptyObjectProvider(),
            emptyObjectProvider());

        assertEquals(300L, facade.createJob(jobParametersDTO, 1L, PlatformType.AUTOMATION));
    }

    @Test
    void testCreateJobAdmittedOverCapWhenOverageEnabled() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, new BigDecimal("10.00"), null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null,
            null, null, null, null, null);

        PlanLimitsProvider planLimitsProvider = tenantId -> planLimits;
        PlanSpendProvider planSpendProvider = tenantId -> new BigDecimal("15.00");
        // $5 over the cap, $100 unbilled tolerance: admitted under the opt-in overage terms.
        PlanOveragePolicyProvider planOveragePolicyProvider =
            tenantId -> new PlanOveragePolicy(true, new BigDecimal("100.00"));

        when(jobFacade.createJob(jobParametersDTO)).thenReturn(400L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(planLimitsProvider),
            objectProviderOf(planOveragePolicyProvider), objectProviderOf(planSpendProvider), emptyObjectProvider(),
            emptyObjectProvider());

        assertEquals(400L, facade.createJob(jobParametersDTO, 1L, PlatformType.AUTOMATION));
    }

    @Test
    void testCreateJobRejectedWhenUnbilledOverageLimitReached() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, new BigDecimal("10.00"), null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null,
            null, null, null, null, null);

        PlanLimitsProvider planLimitsProvider = tenantId -> planLimits;
        // $100 over the cap with a $100 unbilled tolerance: the overage allowance is exhausted, hard stop.
        PlanSpendProvider planSpendProvider = tenantId -> new BigDecimal("110.00");
        PlanOveragePolicyProvider planOveragePolicyProvider =
            tenantId -> new PlanOveragePolicy(true, new BigDecimal("100.00"));

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(planLimitsProvider),
            objectProviderOf(planOveragePolicyProvider), objectProviderOf(planSpendProvider), emptyObjectProvider(),
            emptyObjectProvider());

        assertThrows(
            JobCostLimitExceededException.class,
            () -> facade.createJob(jobParametersDTO, 1L, PlatformType.AUTOMATION));

        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
    }

    @Test
    void testCreateJobSeedsVarsInputWhenResolverPresent() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-1", Map.of("name", "x"), Map.of());

        when(workflowVariablesResolver.resolveForJobPrincipal(7L, PlatformType.AUTOMATION))
            .thenReturn(Map.of("API_URL", "https://api"));
        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(200L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(workflowVariablesResolver));

        facade.createJob(jobParametersDTO, 7L, PlatformType.AUTOMATION);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade).createJob(captor.capture());

        Map<String, Object> inputs = captor.getValue()
            .getInputs();

        assertEquals("x", inputs.get("name"));
        assertEquals(Map.of("API_URL", "https://api"), inputs.get(JobInputConstants.VARIABLES_INPUT));
    }

    @Test
    void testCreateJobDoesNotAddVarsWithoutResolver() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-1", Map.of("name", "x"), Map.of());

        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(200L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider());

        facade.createJob(jobParametersDTO, 7L, PlatformType.AUTOMATION);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade).createJob(captor.capture());
        assertFalse(captor.getValue()
            .getInputs()
            .containsKey(JobInputConstants.VARIABLES_INPUT));
    }

    @Test
    void testCallerSuppliedVarsInputIsOverwritten() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            "wf-1", Map.of(JobInputConstants.VARIABLES_INPUT, Map.of("EVIL", "1")), Map.of());

        when(workflowVariablesResolver.resolveForJobPrincipal(7L, PlatformType.AUTOMATION)).thenReturn(Map.of());
        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(200L);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService,
            emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(), emptyObjectProvider(),
            emptyObjectProvider(), emptyObjectProvider(), objectProviderOf(workflowVariablesResolver));

        facade.createJob(jobParametersDTO, 7L, PlatformType.AUTOMATION);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade).createJob(captor.capture());
        assertEquals(Map.of(), captor.getValue()
            .getInputs()
            .get(JobInputConstants.VARIABLES_INPUT));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyObjectProvider() {
        return (ObjectProvider<T>) mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProviderOf(T instance) {
        ObjectProvider<T> objectProvider = (ObjectProvider<T>) mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(instance);

        return objectProvider;
    }
}
