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

package com.bytechef.platform.billing.service;

import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class BillingUsageServiceImpl implements BillingUsageService {

    private final BillingSubscriptionRepository billingSubscriptionRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BillingUsageServiceImpl(BillingSubscriptionRepository billingSubscriptionRepository) {
        this.billingSubscriptionRepository = billingSubscriptionRepository;
    }

    @Override
    public int countJobExecutionsSince(Instant from, Instant to) {
        return billingSubscriptionRepository.countDeployedJobExecutions(from, to);
    }
}
