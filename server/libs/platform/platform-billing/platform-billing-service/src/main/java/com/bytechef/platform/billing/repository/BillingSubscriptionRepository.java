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

package com.bytechef.platform.billing.repository;

import com.bytechef.platform.billing.domain.BillingSubscription;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Matija Petanjek
 */
@Repository
public interface BillingSubscriptionRepository extends ListCrudRepository<BillingSubscription, Long> {

    // Queries task_execution (atlas-execution) directly — intentional cross-module read for billing usage counting.
    @Query("SELECT COUNT(*) FROM task_execution WHERE status = 2 AND end_date >= :from AND end_date < :to")
    int countCompletedTaskExecutions(Instant from, Instant to);

    Optional<BillingSubscription> findFirstByOrderByCreatedDateDesc();

    Optional<BillingSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
