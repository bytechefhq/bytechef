/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.domain;

import com.bytechef.file.storage.domain.FileEntry;
import java.util.Objects;
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

    @Column
    private FileEntry definition;

    @Column
    private String description;

    @Column
    private String label;

    @Column
    private String name;

    @Id
    private String workflowId;

    private CodeWorkflow() {
    }

    public CodeWorkflow(String workflowId, String name, String label, String description, FileEntry definition) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.definition = definition;
        this.workflowId = workflowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CodeWorkflow codeWorkflow)) {
            return false;
        }

        return Objects.equals(workflowId, codeWorkflow.workflowId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getDescription() {
        return description;
    }

    public FileEntry getDefinition() {
        return definition;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public String toString() {
        return "CodeWorkflow{" +
            "workflowId='" + workflowId + '\'' +
            ", name='" + name + '\'' +
            ", label='" + label + '\'' +
            ", definition='" + definition + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
