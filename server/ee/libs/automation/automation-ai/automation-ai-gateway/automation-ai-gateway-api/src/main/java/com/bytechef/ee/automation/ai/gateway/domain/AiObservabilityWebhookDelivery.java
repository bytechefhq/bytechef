/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_webhook_delivery")
public class AiObservabilityWebhookDelivery {

    @Column("attempt_count")
    private int attemptCount;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("delivered_date")
    private Instant deliveredDate;

    @Column("error_message")
    private String errorMessage;

    @Column("event_type")
    private String eventType;

    @Column("http_status")
    private Integer httpStatus;

    @Id
    private Long id;

    @Column
    private String payload;

    @Column
    private int status;

    @Column("subscription_id")
    private Long subscriptionId;

    public AiObservabilityWebhookDelivery() {
    }

    public AiObservabilityWebhookDelivery(Long subscriptionId, String eventType, String payload) {
        Validate.notNull(subscriptionId, "subscriptionId must not be null");

        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = AiObservabilityWebhookDeliveryStatus.PENDING.ordinal();
        this.attemptCount = 0;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityWebhookDelivery other)) {
            return false;
        }

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getDeliveredDate() {
        return deliveredDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getEventType() {
        return eventType;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public Long getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public AiObservabilityWebhookDeliveryStatus getStatus() {
        return AiObservabilityWebhookDeliveryStatus.values()[status];
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setDeliveredDate(Instant deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setStatus(AiObservabilityWebhookDeliveryStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
