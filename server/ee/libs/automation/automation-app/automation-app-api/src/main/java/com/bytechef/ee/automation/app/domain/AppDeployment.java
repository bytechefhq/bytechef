/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One release of an app version to an app instance. The row is never changed once its status is terminal.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_deployment")
public class AppDeployment {

    @Column("app_instance_id")
    private AggregateReference<AppInstance, Long> appInstanceId;

    @Column("app_version_id")
    private AggregateReference<AppVersion, Long> appVersionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("error_message")
    private @Nullable String errorMessage;

    @Column("finished_date")
    private @Nullable Instant finishedDate;

    @Id
    private Long id;

    @Column
    private @Nullable MapWrapper metadata;

    @Column
    private int provider;

    @Column("provider_deployment_id")
    private @Nullable String providerDeploymentId;

    @Column
    private int status;

    @Column
    private @Nullable String url;

    @Version
    private int version;

    public AppDeployment() {
    }

    public Long getAppInstanceId() {
        return appInstanceId.getId();
    }

    public void setAppInstanceId(Long appInstanceId) {
        this.appInstanceId = AggregateReference.to(appInstanceId);
    }

    public Long getAppVersionId() {
        return appVersionId.getId();
    }

    public void setAppVersionId(Long appVersionId) {
        this.appVersionId = AggregateReference.to(appVersionId);
    }

    public Map<String, ?> getMetadata() {
        return metadata == null ? Map.of() : metadata.getMap();
    }

    public void setMetadata(@Nullable Map<String, ?> metadata) {
        this.metadata = metadata == null ? null : new MapWrapper(metadata);
    }

    public AppDeploymentProviderType getProvider() {
        return AppDeploymentProviderType.values()[provider];
    }

    public void setProvider(AppDeploymentProviderType provider) {
        this.provider = provider.ordinal();
    }

    public AppDeploymentStatus getStatus() {
        return AppDeploymentStatus.values()[status];
    }

    public void setStatus(AppDeploymentStatus status) {
        this.status = status.ordinal();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public @Nullable Instant getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(@Nullable Instant finishedDate) {
        this.finishedDate = finishedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @Nullable String getProviderDeploymentId() {
        return providerDeploymentId;
    }

    public void setProviderDeploymentId(@Nullable String providerDeploymentId) {
        this.providerDeploymentId = providerDeploymentId;
    }

    public @Nullable String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppDeployment that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AppDeployment{id=" + id + ", appInstanceId="
            + (appInstanceId == null ? null : appInstanceId.getId()) + ", appVersionId="
            + (appVersionId == null ? null : appVersionId.getId()) + ", provider=" + provider + ", status=" + status
            + '}';
    }
}
