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
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

/**
 * Atomic counter updates for the PREPAID model. Each statement is a single conditional UPDATE, so concurrent jobs and
 * webhook handlers never lose an increment or a grant.
 *
 * @author Matija Petanjek
 */
@org.springframework.stereotype.Repository
public interface PrepaidBillingSubscriptionRepository extends Repository<BillingSubscription, Long> {

    @Modifying
    @Query("""
        UPDATE billing_subscription SET plan_unit_used = plan_unit_used + 1
        WHERE id = :id AND plan_unit_used < plan_unit_total
        """)
    int incrementPlanUnitUsed(long id);

    @Modifying
    @Query("""
        UPDATE billing_subscription SET extra_unit_used = extra_unit_used + 1
        WHERE id = :id AND extra_unit_used < extra_unit_total
        """)
    int incrementExtraUnitUsed(long id);

    @Modifying
    @Query("UPDATE billing_subscription SET plan_unit_used = 0 WHERE id = :id")
    void resetPlanUnitUsed(long id);

    // On renewal the consumed extra units are dropped from the total, so it holds only what is left to spend instead
    // of growing with every purchased package. One statement, so a concurrent increment cannot be lost.
    @Modifying
    @Query("""
        UPDATE billing_subscription
        SET plan_unit_used = 0, extra_unit_total = extra_unit_total - extra_unit_used, extra_unit_used = 0
        WHERE id = :id
        """)
    void resetUnitUsedForNewPeriod(long id);

    @Modifying
    @Query("UPDATE billing_subscription SET extra_unit_total = extra_unit_total + :units WHERE id = :id")
    void addExtraUnitTotal(long id, int units);

    // platform-billing queries principal_job (platform-workflow-execution) directly — intentional
    // cross-module read, as in BillingSubscriptionRepository.countDeployedJobExecutions. A principal_job row exists
    // only for deployed jobs (AUTOMATION or EMBEDDED); editor test-run jobs never get one.
    @Query("SELECT EXISTS (SELECT 1 FROM principal_job WHERE job_id = :jobId)")
    boolean existsPrincipalJobByJobId(long jobId);
}
