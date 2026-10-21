/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An API Platform collection bound to an app. The collection is referenced by id only: when it is deleted the binding
 * stays and the collection is unavailable to the app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_api_collection")
public class AppApiCollection {

    @Column("api_collection_id")
    private Long apiCollectionId;

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @Id
    private Long id;

    @Version
    private int version;

    public AppApiCollection() {
    }

    public Long getApiCollectionId() {
        return apiCollectionId;
    }

    public void setApiCollectionId(Long apiCollectionId) {
        this.apiCollectionId = apiCollectionId;
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

        if (!(object instanceof AppApiCollection that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
