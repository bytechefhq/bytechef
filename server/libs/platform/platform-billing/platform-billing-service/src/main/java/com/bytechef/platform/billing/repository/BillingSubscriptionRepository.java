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

    // platform-billing queries job/principal_job (platform-workflow-execution) directly — intentional
    // cross-module read for billing usage counting. The join to principal_job (any PlatformType, i.e.
    // AUTOMATION or EMBEDDED) excludes editor test-run jobs, which never get a principal_job row.
    // status IN (2, 3, 4) is Job.Status COMPLETED, FAILED, STOPPED — every terminal outcome, not just
    // successful ones.
    @Query("""
        SELECT COUNT(*) FROM job j
        JOIN principal_job pj ON pj.job_id = j.id
        WHERE j.status IN (2, 3, 4) AND j.end_date >= :from AND j.end_date < :to
        """)
    int countDeployedJobExecutions(Instant from, Instant to);

    Optional<BillingSubscription> findFirstByOrderByCreatedDateDesc();

    Optional<BillingSubscription> findFirstByPlanNameOrderByCreatedDateDesc(String planName);

    Optional<BillingSubscription> findBySubscriptionId(String subscriptionId);
}
