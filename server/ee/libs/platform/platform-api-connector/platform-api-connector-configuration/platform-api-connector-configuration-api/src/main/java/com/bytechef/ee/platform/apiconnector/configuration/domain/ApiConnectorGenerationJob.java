/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.domain;

import java.time.LocalDateTime;

/**
 * Represents an asynchronous API connector generation job.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiConnectorGenerationJob {

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private String id;
    private Status status;
    private String documentationUrl;
    private String specification;
    private String errorMessage;
    private LocalDateTime createdDate;
    private LocalDateTime completedDate;
    private volatile boolean cancellationRequested;

    public ApiConnectorGenerationJob() {
    }

    public ApiConnectorGenerationJob(String id, String documentationUrl) {
        this.id = id;
        this.documentationUrl = documentationUrl;
        this.status = Status.PENDING;
        this.createdDate = LocalDateTime.now();
        this.cancellationRequested = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;

        if (status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED) {
            this.completedDate = LocalDateTime.now();
        }
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested;
    }

    public void setCancellationRequested(boolean cancellationRequested) {
        this.cancellationRequested = cancellationRequested;
    }
}
