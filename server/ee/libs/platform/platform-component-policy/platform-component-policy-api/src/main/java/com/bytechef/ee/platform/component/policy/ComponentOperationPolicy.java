/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A deny-list row disabling a single action or trigger of a component tenant-wide. Presence of a row means the
 * operation is disabled; absence means enabled. See the per-action component policies design spec.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("component_operation_policy")
public class ComponentOperationPolicy {

    /**
     * Operation kind. INT ordinal persisted — append new values at the end, never reorder.
     */
    public enum OperationType {
        ACTION, TRIGGER
    }

    @Id
    private Long id;

    @Column("component_name")
    private String componentName;

    @Column("operation_type")
    private int operationType;

    @Column("operation_name")
    private String operationName;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public ComponentOperationPolicy() {
    }

    public ComponentOperationPolicy(String componentName, OperationType operationType, String operationName) {
        this.componentName = componentName;
        this.operationType = operationType.ordinal();
        this.operationName = operationName;
    }

    public static String key(String componentName, OperationType operationType, String operationName) {
        return componentName + "#" + operationType.name() + "#" + operationName;
    }

    public Long getId() {
        return id;
    }

    public String getComponentName() {
        return componentName;
    }

    public OperationType getOperationType() {
        return OperationType.values()[operationType];
    }

    public String getOperationName() {
        return operationName;
    }

    public String toKey() {
        return key(componentName, getOperationType(), operationName);
    }
}
