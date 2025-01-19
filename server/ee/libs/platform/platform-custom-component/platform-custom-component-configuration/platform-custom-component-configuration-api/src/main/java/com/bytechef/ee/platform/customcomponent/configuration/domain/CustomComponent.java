/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.domain;

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("custom_component")
public class CustomComponent {

    public enum Language {
        JAVA("jar"), JAVASCRIPT("js"), PYTHON("py"), RUBY("rb");

        private final String extension;

        Language(String extension) {
            this.extension = extension;
        }

        public static Language of(@NonNull String filename) {
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

    @Column("component_file")
    private FileEntry componentFile;

    @Column("component_version")
    private int componentVersion;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String description;

    @Column
    private boolean enabled;

    @Column
    private String icon;

    @Id
    private Long id;

    @Column
    private int language;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private String title;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CustomComponent customComponent)) {
            return false;
        }

        return Objects.equals(id, customComponent.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public FileEntry getComponentFile() {
        return componentFile;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getIcon() {
        return icon;
    }

    public Long getId() {
        return id;
    }

    public Language getLanguage() {
        return Language.values()[language];
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getVersion() {
        return version;
    }

    public void setComponentFile(FileEntry componentFile) {
        this.componentFile = componentFile;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "CodeWorkflow{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", componentVersion=" + componentVersion +
            ", componentFile=" + componentFile +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
