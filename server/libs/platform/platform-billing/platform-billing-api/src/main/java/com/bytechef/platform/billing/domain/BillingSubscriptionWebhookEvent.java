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
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Matija Petanjek
 */
@Table("billing_subscription_webhook_event")
public class BillingSubscriptionWebhookEvent {

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("event_type")
    private String eventType;

    @Id
    private Long id;

    @Column("stripe_event_id")
    private String stripeEventId;

    @Column("subscription_id")
    private Long subscriptionId;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof BillingSubscriptionWebhookEvent that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getId() {
        return id;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStripeEventId(String stripeEventId) {
        this.stripeEventId = stripeEventId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public String toString() {
        return "BillingSubscriptionWebhookEvent{" +
            "id=" + id +
            ", stripeEventId='" + stripeEventId + '\'' +
            ", eventType='" + eventType + '\'' +
            ", subscriptionId=" + subscriptionId +
            ", createdDate=" + createdDate +
            '}';
    }
}
