/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("integration_version")
public class IntegrationVersion {

    public enum Status {

        DRAFT, PUBLISHED
    }

    @Column
    private String description;

    @Column("published_date")
    private Instant publishedDate;

    @Column
    private int status = Status.DRAFT.ordinal();

    @Column
    private int version;

    private IntegrationVersion() {
    }

    public IntegrationVersion(int version) {
        this.version = version;
    }

    public IntegrationVersion(int version, int status, Instant publishedDate, String description) {
        this.description = description;
        this.publishedDate = publishedDate;
        this.status = status;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IntegrationVersion that)) {
            return false;
        }

        return status == that.status && version == that.version && Objects.equals(description, that.description)
            && Objects.equals(publishedDate, that.publishedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, publishedDate, status, version);
    }

    public String getDescription() {
        return description;
    }

    public Instant getPublishedDate() {
        return publishedDate;
    }

    public Status getStatus() {
        return Status.values()[status];
    }

    public int getVersion() {
        return version;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setStatus(Status status) {
        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "IntegrationVersion{" +
            "description='" + description + '\'' +
            ", publishedDate=" + publishedDate +
            ", status=" + status +
            ", version=" + version +
            '}';
    }
}
