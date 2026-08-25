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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanOveragePolicy;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.plan.provider.PlanOveragePolicyProvider;
import com.bytechef.platform.plan.provider.PlanSpendProvider;
import com.bytechef.platform.ratelimit.ConcurrentExecutionGate;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.platform.ratelimit.RateLimitPolicy;
import com.bytechef.platform.ratelimit.RateLimiter;
import com.bytechef.platform.variable.WorkflowVariablesResolver;
import com.bytechef.platform.workflow.JobInputConstants;
import com.bytechef.platform.workflow.execution.exception.JobConcurrencyLimitExceededException;
import com.bytechef.platform.workflow.execution.exception.JobCostLimitExceededException;
import com.bytechef.platform.workflow.execution.exception.JobRateLimitExceededException;
import com.bytechef.platform.workflow.execution.service.LicenceJobUsageService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class PrincipalJobFacadeImpl implements PrincipalJobFacade {

    private static final Logger log = LoggerFactory.getLogger(PrincipalJobFacadeImpl.class);

    private final PrincipalJobService principalJobService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final WorkflowService workflowService;
    private final LicenceJobUsageService licenceJobUsageService;
    private final ObjectProvider<ConcurrentExecutionGate> concurrentExecutionGateProvider;
    private final ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider;
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private final ObjectProvider<PlanOveragePolicyProvider> planOveragePolicyProviderObjectProvider;
    private final ObjectProvider<PlanSpendProvider> planSpendProviderObjectProvider;
    private final ObjectProvider<RateLimiter> rateLimiterObjectProvider;
    private final ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider;

    @SuppressFBWarnings("EI")
    public PrincipalJobFacadeImpl(
        PrincipalJobService principalJobService, JobFacade jobFacade, JobService jobService,
        WorkflowService workflowService, LicenceJobUsageService licenceJobUsageService,
        ObjectProvider<ConcurrentExecutionGate> concurrentExecutionGateProvider,
        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider,
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        ObjectProvider<PlanOveragePolicyProvider> planOveragePolicyProviderObjectProvider,
        ObjectProvider<PlanSpendProvider> planSpendProviderObjectProvider,
        ObjectProvider<RateLimiter> rateLimiterObjectProvider,
        ObjectProvider<WorkflowVariablesResolver> workflowVariablesResolverObjectProvider) {

        this.principalJobService = principalJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.workflowService = workflowService;
        this.licenceJobUsageService = licenceJobUsageService;
        this.concurrentExecutionGateProvider = concurrentExecutionGateProvider;
        this.planLimitRejectionCounterObjectProvider = planLimitRejectionCounterObjectProvider;
        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
        this.planOveragePolicyProviderObjectProvider = planOveragePolicyProviderObjectProvider;
        this.planSpendProviderObjectProvider = planSpendProviderObjectProvider;
        this.rateLimiterObjectProvider = rateLimiterObjectProvider;
        this.workflowVariablesResolverObjectProvider = workflowVariablesResolverObjectProvider;
    }

    /**
     * Copies {@code jobParametersDTO} with the principal's variables snapshot under
     * {@link JobInputConstants#VARIABLES_INPUT}. No resolver bean (CE) leaves the inputs untouched; with a resolver
     * (EE) the key is always written -- an empty map included -- and overwrites any caller-supplied {@code vars}, since
     * the name is reserved.
     */
    private JobParametersDTO withVariables(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        WorkflowVariablesResolver workflowVariablesResolver = workflowVariablesResolverObjectProvider.getIfAvailable();

        if (workflowVariablesResolver == null) {
            return jobParametersDTO;
        }

        Map<String, Object> inputs = new HashMap<>(jobParametersDTO.getInputs());

        inputs.put(
            JobInputConstants.VARIABLES_INPUT, workflowVariablesResolver.resolveForJobPrincipal(jobPrincipalId, type));

        return new JobParametersDTO(
            jobParametersDTO.getWorkflowId(), jobParametersDTO.getParentTaskExecutionId(), inputs,
            jobParametersDTO.getLabel(), jobParametersDTO.getPriority(), jobParametersDTO.getWebhooks(),
            jobParametersDTO.getMetadata());
    }

    private void countRejection(String limit) {
        PlanLimitRejectionCounter planLimitRejectionCounter = planLimitRejectionCounterObjectProvider.getIfAvailable();

        if (planLimitRejectionCounter != null) {
            planLimitRejectionCounter.increment(limit);
        }
    }

    /**
     * Plan-level concurrent-execution admission (Sim model: a slot is held from admission to terminal status, released
     * by the coordinator's terminal-status listener). No gate/provider bean or a null limit (the SELF_HOSTED default)
     * admits unconditionally — pre-plan behavior. Applied on the async dispatch path only: sync executions
     * ({@code createJobWithoutDispatch}) run on a caller thread whose completion events do not always traverse the
     * coordinator fan-out, so gating them would risk leaked slots.
     */
    private void admitConcurrentExecution() {
        ConcurrentExecutionGate concurrentExecutionGate = concurrentExecutionGateProvider.getIfAvailable();
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (concurrentExecutionGate == null || planLimitsProvider == null) {
            return;
        }

        String tenantId = TenantContext.getCurrentTenantId();

        Integer maxConcurrentExecutions = planLimitsProvider.getPlanLimits(tenantId)
            .maxConcurrentExecutions();

        if (maxConcurrentExecutions == null) {
            return;
        }

        if (!concurrentExecutionGate.tryAcquire("executions:" + tenantId, maxConcurrentExecutions)) {
            countRejection("concurrency");

            throw new JobConcurrencyLimitExceededException(maxConcurrentExecutions);
        }
    }

    /**
     * Plan-level async submissions-per-minute admission (Sim model: sustained rate x burst multiplier, token bucket).
     * Checked BEFORE the concurrency slot so a rate-rejected submission never acquires a slot it would then leak. No
     * limiter/provider bean or a null limit (the SELF_HOSTED default) admits unconditionally.
     */
    private void admitAsyncSubmissionRate() {
        RateLimiter rateLimiter = rateLimiterObjectProvider.getIfAvailable();
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (rateLimiter == null || planLimitsProvider == null) {
            return;
        }

        String tenantId = TenantContext.getCurrentTenantId();

        PlanLimits planLimits = planLimitsProvider.getPlanLimits(tenantId);

        Integer asyncRequestsPerMinute = planLimits.asyncRequestsPerMinute();

        if (asyncRequestsPerMinute == null) {
            return;
        }

        RateLimitPolicy rateLimitPolicy = new RateLimitPolicy(asyncRequestsPerMinute, planLimits.burstMultiplier());

        if (!rateLimiter.tryConsume("async:" + tenantId, rateLimitPolicy)) {
            countRejection("async");

            throw new JobRateLimitExceededException(asyncRequestsPerMinute);
        }
    }

    /**
     * Plan-level monthly-cost admission (Sim model: submissions are refused once the tenant's current-period execution
     * spend has reached the plan's included monthly cost). No spend provider or plan-limits bean, or a null cost limit
     * (the SELF_HOSTED default), admits unconditionally. Checked after the cheap local rate check and before the
     * concurrency slot so a cost-rejected submission never acquires a slot it would then leak; the spend provider is
     * expected to cache, so this does not run a SUM per submission.
     */
    private void admitMonthlyCostCap() {
        PlanSpendProvider planSpendProvider = planSpendProviderObjectProvider.getIfAvailable();
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (planSpendProvider == null || planLimitsProvider == null) {
            return;
        }

        String tenantId = TenantContext.getCurrentTenantId();

        BigDecimal includedMonthlyCostUsd = planLimitsProvider.getPlanLimits(tenantId)
            .includedMonthlyCostUsd();

        if (includedMonthlyCostUsd == null) {
            return;
        }

        BigDecimal currentPeriodSpendUsd = planSpendProvider.getCurrentPeriodSpendUsd(tenantId);

        if (currentPeriodSpendUsd.compareTo(includedMonthlyCostUsd) >= 0) {
            if (isOverageAdmitted(tenantId, includedMonthlyCostUsd, currentPeriodSpendUsd)) {
                return;
            }

            countRejection("cost");

            throw new JobCostLimitExceededException(includedMonthlyCostUsd);
        }
    }

    /**
     * Whether an over-cap submission is admitted under the tenant's on-demand overage terms (Sim's opt-in overage
     * model). Without a {@link PlanOveragePolicyProvider} bean — the placeholder state until the billing integration
     * lands — overage is disabled and the cap hard-stops.
     */
    private boolean isOverageAdmitted(
        String tenantId, BigDecimal includedMonthlyCostUsd, BigDecimal currentPeriodSpendUsd) {

        PlanOveragePolicyProvider planOveragePolicyProvider =
            planOveragePolicyProviderObjectProvider.getIfAvailable();

        if (planOveragePolicyProvider == null) {
            return false;
        }

        PlanOveragePolicy planOveragePolicy = planOveragePolicyProvider.getOveragePolicy(tenantId);

        if (!planOveragePolicy.enabled()) {
            return false;
        }

        BigDecimal unbilledLimitUsd = planOveragePolicy.unbilledLimitUsd();

        if (unbilledLimitUsd == null) {
            return true;
        }

        BigDecimal unbilledOverageUsd = currentPeriodSpendUsd.subtract(includedMonthlyCostUsd);

        return unbilledOverageUsd.compareTo(unbilledLimitUsd) < 0;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {
        Optional<Long> principalId = principalJobService.fetchJobPrincipalId(parentJobId, platformType);

        long childJobId = jobFacade.createJob(
            principalId.map(id -> withVariables(jobParametersDTO, id, platformType))
                .orElse(jobParametersDTO));

        if (principalId.isPresent()) {
            principalJobService.create(childJobId, principalId.get(), platformType);
        } else {
            log.warn(
                "No principal found for parent job {} -- child job {} will have no principal association",
                parentJobId, childJobId);
        }

        return childJobId;
    }

    @Override
    // TODO @Transactional
    public long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        licenceJobUsageService.consumeOrThrow();

        admitAsyncSubmissionRate();
        admitMonthlyCostCap();
        admitConcurrentExecution();

        long jobId = jobFacade.createJob(withVariables(jobParametersDTO, jobPrincipalId, type));

        principalJobService.create(jobId, jobPrincipalId, type);

        return jobId;
    }

    @Override
    @Transactional
    public long createPrincipalLinkedJob(
        long referenceJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {

        // The contract of this method is "linked to the same principal instance as referenceJobId." If we cannot
        // establish the linkage, refusing to create the job is strictly safer than silently creating an orphan
        // (no tenant attribution, no billing, invisible to workspace-scoped lookups). The caller is the agent-tool
        // sub-workflow bridge; failing here surfaces as a coordinator-level error rather than a silent multi-tenant
        // leak. See review finding C3.

        long principalId = principalJobService.fetchJobPrincipalId(referenceJobId, platformType)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot create principal-linked job: no principal found for reference job %d (type=%s)"
                    .formatted(referenceJobId, platformType)));

        long jobId = jobFacade.createJob(withVariables(jobParametersDTO, principalId, platformType));

        principalJobService.create(jobId, principalId, platformType);

        return jobId;
    }

    @Override
    @Transactional
    public Job createJobWithoutDispatch(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        licenceJobUsageService.consumeOrThrow();

        Job job = jobService.create(
            withVariables(jobParametersDTO, jobPrincipalId, type),
            workflowService.getWorkflow(jobParametersDTO.getWorkflowId()));

        principalJobService.create(Validate.notNull(job.getId(), "id"), jobPrincipalId, type);

        return job;
    }
}
