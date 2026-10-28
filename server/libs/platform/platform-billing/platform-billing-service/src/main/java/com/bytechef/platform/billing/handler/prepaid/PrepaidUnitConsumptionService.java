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

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.repository.PrepaidBillingSubscriptionRepository;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes one prepaid job-execution unit for a started job. Only deployed jobs (those with a {@code principal_job}
 * row) are ever counted; the plan pool is drained before the extra pool.
 *
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class PrepaidUnitConsumptionService {

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

    @Transactional
    public void consumeUnit(long jobId) {
        if (!prepaidBillingSubscriptionRepository.existsPrincipalJobByJobId(jobId)) {
            return;
        }

        Optional<BillingSubscription> currentSubscriptionOptional =
            billingSubscriptionService.fetchCurrentSubscription();

        if (currentSubscriptionOptional.isEmpty()) {
            return;
        }

        BillingSubscription billingSubscription = currentSubscriptionOptional.get();

        if (billingSubscription.getSubscriptionModel() != SubscriptionModel.PREPAID ||
            "TRIAL".equals(billingSubscription.getPlanName())) {

            return;
        }

        long subscriptionId = billingSubscription.getId();

        if (prepaidBillingSubscriptionRepository.incrementPlanUnitUsed(subscriptionId) == 0 &&
            prepaidBillingSubscriptionRepository.incrementExtraUnitUsed(subscriptionId) == 0) {

            log.warn("Job {} started with no prepaid units left; nothing was counted", jobId);
        }
    }
}
