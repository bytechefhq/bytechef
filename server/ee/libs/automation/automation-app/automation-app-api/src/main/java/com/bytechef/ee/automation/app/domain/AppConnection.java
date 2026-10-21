/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A connection bound to an app under a binding key, with the actions and tools the app may call through it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_connection")
public class AppConnection {

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @Column("binding_key")
    private String bindingKey;

    @Column("connection_id")
    private Long connectionId;

    @Id
    private Long id;

    @MappedCollection(idColumn = "app_connection_id")
    private Set<AppConnectionOperation> operations = new HashSet<>();

    @Column("proxy_allowed_methods")
    private String proxyAllowedMethods = "";

    @Column("proxy_enabled")
    private boolean proxyEnabled;

    @Version
    private int version;

    public AppConnection() {
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
    }

    public boolean allows(AppOperationType type, String operationName) {
        return operations.contains(new AppConnectionOperation(type, operationName));
    }

    public List<String> getOperationNames(AppOperationType type) {
        return operations.stream()
            .filter(operation -> operation.getOperationType() == type)
            .map(AppConnectionOperation::getOperationName)
            .sorted()
            .toList();
    }

    public void setOperations(List<String> actionNames, List<String> toolNames) {
        operations = new HashSet<>();

        for (String actionName : actionNames) {
            operations.add(new AppConnectionOperation(AppOperationType.ACTION, actionName));
        }

        for (String toolName : toolNames) {
            operations.add(new AppConnectionOperation(AppOperationType.TOOL, toolName));
        }
    }

    public List<String> getProxyAllowedMethodList() {
        if (proxyAllowedMethods == null || proxyAllowedMethods.isBlank()) {
            return List.of();
        }

        return List.of(proxyAllowedMethods.split(","));
    }

    public void setProxyAllowedMethodList(List<String> methods) {
        this.proxyAllowedMethods = String.join(",", methods);
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public void setProxyEnabled(boolean proxyEnabled) {
        this.proxyEnabled = proxyEnabled;
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

        if (!(object instanceof AppConnection that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
