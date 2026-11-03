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

package com.bytechef.platform.billing.handler.prepaid;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobStartGuard;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.repository.PrepaidBillingSubscriptionRepository;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes one prepaid job-execution unit when a job starts, and refuses the start when both pools are exhausted. Only
 * deployed top-level jobs (those with a {@code principal_job} row and no parent) are ever counted; subflow jobs run on
 * their parent's unit. The plan pool is drained before the extra pool.
 *
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class PrepaidUnitConsumptionService implements JobStartGuard {

    private static final Logger log = LoggerFactory.getLogger(PrepaidUnitConsumptionService.class);

    private final BillingSubscriptionService billingSubscriptionService;
    private final PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PrepaidUnitConsumptionService(
        BillingSubscriptionService billingSubscriptionService,
        PrepaidBillingSubscriptionRepository prepaidBillingSubscriptionRepository) {

        this.billingSubscriptionService = billingSubscriptionService;
        this.prepaidBillingSubscriptionRepository = prepaidBillingSubscriptionRepository;
    }

    @Override
    @Transactional
    public boolean tryStart(Job job) {
        long jobId = Objects.requireNonNull(job.getId(), "id");

        if (job.getParentTaskExecutionId() != null ||
            !prepaidBillingSubscriptionRepository.existsPrincipalJobByJobId(jobId)) {

            return true;
        }

        BillingSubscription billingSubscription = billingSubscriptionService.fetchCurrentSubscription()
            .orElseThrow(() -> new IllegalStateException("No subscription found, TRIAL fallback is missing"));

        // With no paid subscription the tenant falls back to trial terms; an ended trial (CANCELED) runs no jobs.
        if (billingSubscription.getStatus() == BillingSubscription.Status.CANCELED) {
            log.warn("Job {} refused: subscription {} is canceled", jobId, billingSubscription.getId());

            return false;
        }

        if (billingSubscription.getSubscriptionModel() != SubscriptionModel.PREPAID ||
            "TRIAL".equals(billingSubscription.getPlanName())) {

            return true;
        }

        // Units are granted at period rollover, before the renewal invoice is paid; a failed payment shows up as a
        // non-active status (past_due / unpaid / incomplete), so stop spending units until Stripe reports ACTIVE again.
        if (billingSubscription.getStatus() != BillingSubscription.Status.ACTIVE) {
            log.warn(
                "Job {} refused: subscription {} is {}", jobId, billingSubscription.getId(),
                billingSubscription.getStatus());

            return false;
        }

        long subscriptionId = billingSubscription.getId();

        if (prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId) == 0 &&
            prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(subscriptionId) == 0) {

            log.warn("Job {} refused: no prepaid units left", jobId);

            return false;
        }

        return true;
    }
}
