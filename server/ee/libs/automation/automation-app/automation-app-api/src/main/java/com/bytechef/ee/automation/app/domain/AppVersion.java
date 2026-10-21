/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.time.Instant;
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
 * A named label for code of an app that ByteChef does not hold, unique per app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_version")
public class AppVersion {

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private @Nullable String description;

    @Column("git_commit_sha")
    private @Nullable String gitCommitSha;

    @Column("git_ref")
    private @Nullable String gitRef;

    @Id
    private Long id;

    @Column
    private String name;

    @Version
    private int version;

    public AppVersion() {
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
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

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public @Nullable String getGitCommitSha() {
        return gitCommitSha;
    }

    public void setGitCommitSha(@Nullable String gitCommitSha) {
        this.gitCommitSha = gitCommitSha;
    }

    public @Nullable String getGitRef() {
        return gitRef;
    }

    public void setGitRef(@Nullable String gitRef) {
        this.gitRef = gitRef;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

        if (!(object instanceof AppVersion that)) {
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
        return "AppVersion{id=" + id + ", appId=" + (appId == null ? null : appId.getId()) + ", name='" + name
            + "', gitCommitSha='" + gitCommitSha + "'}";
    }
}
