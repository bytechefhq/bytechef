/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.domain;

import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("code_workflow")
public class CodeWorkflow {

    @Id
    private UUID id;

    @Column
    private String name;

    private CodeWorkflow() {
    }

    public CodeWorkflow(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CodeWorkflow codeWorkflow)) {
            return false;
        }

        return Objects.equals(id, codeWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getId() {
        return id.toString();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CodeWorkflow{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            '}';
    }
}
