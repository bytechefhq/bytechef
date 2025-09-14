/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.domain;

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("code_workflow_container")
public class CodeWorkflowContainer {

    public enum Language {
        JAVA("jar"), JAVASCRIPT("js"), PYTHON("py"), RUBY("rb");

        private final String extension;

        Language(String extension) {
            this.extension = extension;
        }

        public static Language of(String filename) {
            if (filename.endsWith(".jar")) {
                return JAVA;
            } else if (filename.endsWith(".js")) {
                return JAVASCRIPT;
            } else if (filename.endsWith(".py")) {
                return PYTHON;
            } else if (filename.endsWith(".rb")) {
                return RUBY;
            }

            throw new IllegalArgumentException("Unsupported language : " + filename);
        }

        public String getExtension() {
            return extension;
        }
    }

    private CodeWorkflowContainer() {
    }

    public CodeWorkflowContainer(UUID uuid) {
        this.uuid = uuid;
    }

    @MappedCollection(idColumn = "code_workflow_container_id")
    private Set<CodeWorkflow> codeWorkflows = new HashSet<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("external_version")
    private String externalVersion;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private int language;

    @Column
    private String name;

    @Column("uuid")
    private UUID uuid;

    @Column("workflows")
    private FileEntry workflows;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CodeWorkflowContainer codeWorkflow)) {
            return false;
        }

        return Objects.equals(id, codeWorkflow.id);
    }

    public void addCodeWorkflow(UUID id, String name) {
        codeWorkflows.add(new CodeWorkflow(id, name));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getUuid() {
        return uuid.toString();
    }

    public Long getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getExternalVersion() {
        return externalVersion;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Language getLanguage() {
        return Language.values()[language];
    }

    public String getName() {
        return name;
    }

    public FileEntry getWorkflows() {
        return workflows;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, String> getWorkflowNameIds() {
        return codeWorkflows.stream()
            .collect(Collectors.toMap(CodeWorkflow::getName, CodeWorkflow::getId));
    }

    public void setUuid(String uuid) {
        this.uuid = UUID.fromString(uuid);
    }

    public void setExternalVersion(String externalVersion) {
        this.externalVersion = externalVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLanguage(Language language) {
        this.language = language.ordinal();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkflows(FileEntry workflows) {
        this.workflows = workflows;
    }

    @Override
    public String toString() {
        return "CodeWorkflow{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", uuid='" + uuid + '\'' +
            ", externalVersion='" + externalVersion + '\'' +
            ", language='" + language + '\'' +
            ", workflowsFile='" + workflows + '\'' +
            ", externalVersion=" + externalVersion +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
