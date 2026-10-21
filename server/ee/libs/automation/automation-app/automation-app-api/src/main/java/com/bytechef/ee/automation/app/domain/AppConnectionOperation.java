/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One allow-list entry of a binding. No {@code @Id}: the composite key is (app_connection_id, operation_type,
 * operation_name) in Liquibase.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_connection_operation")
public class AppConnectionOperation {

    @Column("operation_name")
    private String operationName;

    @Column("operation_type")
    private int operationType;

    private AppConnectionOperation() {
    }

    public AppConnectionOperation(AppOperationType operationType, String operationName) {
        this.operationType = operationType.ordinal();
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }

    public AppOperationType getOperationType() {
        return AppOperationType.values()[operationType];
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppConnectionOperation that)) {
            return false;
        }

        return operationType == that.operationType && Objects.equals(operationName, that.operationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationType, operationName);
    }
}
