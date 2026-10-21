/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A configured installation of an app in one workspace and environment. The row holds the instance's one secret key as
 * a hash; the key itself is shown once at create and rotate and never stored.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_instance")
public class AppInstance {

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @Column
    private @Nullable MapWrapper configuration;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("current_app_deployment_id")
    private @Nullable Long currentAppDeploymentId;

    @Column("deployment_connection_id")
    private @Nullable Long deploymentConnectionId;

    @Column("deployment_settings")
    private @Nullable MapWrapper deploymentSettings;

    @Column
    private int environment;

    @Column("hosting_target")
    private int hostingTarget;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("owner_user_id")
    private Long ownerUserId;

    @Column("registered_client_id")
    private @Nullable String registeredClientId;

    @Column("secret_key_hash")
    private String secretKeyHash;

    @Column("secret_key_hint")
    private String secretKeyHint;

    @Column("secret_key_last_used_date")
    private @Nullable Instant secretKeyLastUsedDate;

    @Column
    private int status;

    @Column
    private @Nullable String url;

    @Column("user_auth_mode")
    private int userAuthMode;

    @Column
    private String uuid;

    @Version
    private int version;

    @Column
    private int visibility = ResourceVisibility.WORKSPACE.ordinal();

    @Column("workspace_id")
    private Long workspaceId;

    public AppInstance() {
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
    }

    public Map<String, ?> getConfiguration() {
        return configuration == null ? Map.of() : configuration.getMap();
    }

    public void setConfiguration(@Nullable Map<String, ?> configuration) {
        this.configuration = configuration == null ? null : new MapWrapper(configuration);
    }

    public Map<String, ?> getDeploymentSettings() {
        return deploymentSettings == null ? Map.of() : deploymentSettings.getMap();
    }

    public void setDeploymentSettings(@Nullable Map<String, ?> deploymentSettings) {
        this.deploymentSettings = deploymentSettings == null ? null : new MapWrapper(deploymentSettings);
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public int getEnvironmentId() {
        return environment;
    }

    public AppHostingTarget getHostingTarget() {
        return AppHostingTarget.values()[hostingTarget];
    }

    public void setHostingTarget(AppHostingTarget hostingTarget) {
        this.hostingTarget = hostingTarget.ordinal();
    }

    public AppInstanceStatus getStatus() {
        return AppInstanceStatus.values()[status];
    }

    public void setStatus(AppInstanceStatus status) {
        this.status = status.ordinal();
    }

    public AppUserAuthMode getUserAuthMode() {
        return AppUserAuthMode.values()[userAuthMode];
    }

    public void setUserAuthMode(AppUserAuthMode userAuthMode) {
        this.userAuthMode = userAuthMode.ordinal();
    }

    public ResourceVisibility getVisibility() {
        return ResourceVisibility.values()[visibility];
    }

    public void setVisibility(ResourceVisibility visibility) {
        this.visibility = visibility.ordinal();
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

    public @Nullable Long getCurrentAppDeploymentId() {
        return currentAppDeploymentId;
    }

    public void setCurrentAppDeploymentId(@Nullable Long currentAppDeploymentId) {
        this.currentAppDeploymentId = currentAppDeploymentId;
    }

    public @Nullable Long getDeploymentConnectionId() {
        return deploymentConnectionId;
    }

    public void setDeploymentConnectionId(@Nullable Long deploymentConnectionId) {
        this.deploymentConnectionId = deploymentConnectionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public @Nullable String getRegisteredClientId() {
        return registeredClientId;
    }

    public void setRegisteredClientId(@Nullable String registeredClientId) {
        this.registeredClientId = registeredClientId;
    }

    public String getSecretKeyHash() {
        return secretKeyHash;
    }

    public void setSecretKeyHash(String secretKeyHash) {
        this.secretKeyHash = secretKeyHash;
    }

    public String getSecretKeyHint() {
        return secretKeyHint;
    }

    public void setSecretKeyHint(String secretKeyHint) {
        this.secretKeyHint = secretKeyHint;
    }

    public @Nullable Instant getSecretKeyLastUsedDate() {
        return secretKeyLastUsedDate;
    }

    public void setSecretKeyLastUsedDate(@Nullable Instant secretKeyLastUsedDate) {
        this.secretKeyLastUsedDate = secretKeyLastUsedDate;
    }

    public @Nullable String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppInstance that)) {
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
        return "AppInstance{id=" + id + ", appId=" + (appId == null ? null : appId.getId()) + ", name='" + name
            + "', workspaceId=" + workspaceId + ", environment=" + environment + ", status=" + status + '}';
    }
}
