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

package com.bytechef.platform.billing.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Matija Petanjek
 */
@Table("billing_subscription")
public class BillingSubscription {

    public enum Status {
        INCOMPLETE, INCOMPLETE_EXPIRED, TRIALING, ACTIVE, PAST_DUE, CANCELED, UNPAID, PAUSED, UNKNOWN;

        public static Status fromStripe(String stripeStatus) {
            try {
                return valueOf(stripeStatus.toUpperCase()
                    .replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    @Column("cancel_at_period_end")
    private boolean cancelAtPeriodEnd;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("current_period_end")
    private Instant currentPeriodEnd;

    @Column("current_period_start")
    private Instant currentPeriodStart;

    @Id
    private Long id;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Column("plan_name")
    private String planName;

    @Column("scheduled_plan_name")
    private String scheduledPlanName;

    @Column
    private String status;

    @Column("stripe_customer_id")
    private String stripeCustomerId;

    @Column("stripe_product_id")
    private String stripeProductId;

    @Column("stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column("stripe_usage_product_id")
    private String stripeUsageProductId;

    @Column("task_limit")
    private int taskLimit;

    @Column("last_reported_at")
    private Instant lastReportedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof BillingSubscription billingSubscription)) {
            return false;
        }

        return Objects.equals(id, billingSubscription.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getPlanName() {
        return planName;
    }

    public String getScheduledPlanName() {
        return scheduledPlanName;
    }

    public Status getStatus() {
        return Status.valueOf(status);
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getStripeProductId() {
        return stripeProductId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public String getStripeUsageProductId() {
        return stripeUsageProductId;
    }

    public int getTaskLimit() {
        return taskLimit;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getLastReportedAt() {
        return lastReportedAt;
    }

    public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public void setCurrentPeriodStart(Instant currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public void setScheduledPlanName(String scheduledPlanName) {
        this.scheduledPlanName = scheduledPlanName;
    }

    public void setStatus(Status status) {
        this.status = status.name();
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public void setStripeProductId(String stripeProductId) {
        this.stripeProductId = stripeProductId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public void setStripeUsageProductId(String stripeUsageProductId) {
        this.stripeUsageProductId = stripeUsageProductId;
    }

    public void setTaskLimit(int taskLimit) {
        this.taskLimit = taskLimit;
    }

    public void setLastReportedAt(Instant lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    @Override
    public String toString() {
        return "BillingSubscription{" +
            "id=" + id +
            ", stripeSubscriptionId='" + stripeSubscriptionId + '\'' +
            ", stripeCustomerId='" + stripeCustomerId + '\'' +
            ", status='" + status + '\'' +
            ", planName='" + planName + '\'' +
            ", taskLimit=" + taskLimit +
            ", currentPeriodStart=" + currentPeriodStart +
            ", currentPeriodEnd=" + currentPeriodEnd +
            ", cancelAtPeriodEnd=" + cancelAtPeriodEnd +
            ", lastReportedAt=" + lastReportedAt +
            '}';
    }
}
