/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.assetfile.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 * @author Ivica Cardic
 */
@Table("asset_file")
public class AssetFile {

    @Id
    private Long id;

    private String name;

    private String description;

    private String mimeType;

    private long sizeBytes;

    private FileEntry file;

    private short source;

    private Short generatedByAgentSource;

    private String generatedFromPrompt;

    private Short format;

    private String metadataJson;

    private int environment;

    @Column("workspace_id")
    private Long workspaceId;

    @Column("public_link_token")
    private String publicLinkToken;

    @MappedCollection(idColumn = "asset_file_id")
    private Set<AssetFileTag> assetFileTags = new HashSet<>();

    @CreatedDate
    private Instant createdDate;

    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private Instant lastModifiedDate;

    @LastModifiedBy
    private String lastModifiedBy;

    @Version
    private int version;

    public AssetFile() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public FileEntry getFile() {
        return file;
    }

    public void setFile(FileEntry file) {
        this.file = file;
    }

    public AssetFileSource getSource() {
        return AssetFileSource.fromOrdinal(source);
    }

    public void setSource(AssetFileSource source) {
        this.source = (short) source.ordinal();
    }

    public Short getGeneratedByAgentSource() {
        return generatedByAgentSource;
    }

    public void setGeneratedByAgentSource(Short generatedByAgentSource) {
        if (generatedByAgentSource != null && generatedByAgentSource < 0) {
            throw new IllegalArgumentException(
                "generatedByAgentSource must be a non-negative Source.ordinal()");
        }

        this.generatedByAgentSource = generatedByAgentSource;
    }

    public String getGeneratedFromPrompt() {
        return generatedFromPrompt;
    }

    public void setGeneratedFromPrompt(String generatedFromPrompt) {
        this.generatedFromPrompt = generatedFromPrompt;
    }

    public AssetFileFormat getFormat() {
        return format == null ? null : AssetFileFormat.fromOrdinal(format);
    }

    public void setFormat(AssetFileFormat format) {
        this.format = format == null ? null : (short) format.ordinal();
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getPublicLinkToken() {
        return publicLinkToken;
    }

    public void setPublicLinkToken(String publicLinkToken) {
        this.publicLinkToken = publicLinkToken;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void validate() {
        if (workspaceId == null) {
            throw new IllegalStateException("AssetFile must have a non-null workspaceId");
        }

        AssetFileSource sourceEnum = getSource();

        if (sourceEnum == AssetFileSource.AI_GENERATED && generatedByAgentSource == null) {
            throw new IllegalStateException(
                "AssetFile with source=AI_GENERATED must have a non-null generatedByAgentSource");
        }

        if (sourceEnum == AssetFileSource.USER_UPLOAD) {
            if (generatedByAgentSource != null) {
                throw new IllegalStateException(
                    "AssetFile with source=USER_UPLOAD must not have generatedByAgentSource set");
            }

            if (generatedFromPrompt != null) {
                throw new IllegalStateException(
                    "AssetFile with source=USER_UPLOAD must not have generatedFromPrompt set");
            }
        }
    }

    public List<Long> getTagIds() {
        return assetFileTags
            .stream()
            .map(AssetFileTag::getTagId)
            .toList();
    }

    public void setTagIds(List<Long> tagIds) {
        this.assetFileTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (long tagId : tagIds) {
                assetFileTags.add(new AssetFileTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            setTagIds(List.of());
        } else {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AssetFile that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AssetFile{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", sizeBytes=" + sizeBytes +
            ", file=" + file +
            ", source=" + source +
            ", generatedByAgentSource=" + generatedByAgentSource +
            ", generatedFromPrompt='" + generatedFromPrompt + '\'' +
            ", format=" + format +
            ", metadataJson='" + metadataJson + '\'' +
            ", environment=" + environment +
            ", workspaceId=" + workspaceId +
            ", publicLinkToken='" + (publicLinkToken == null ? null : "***") + '\'' +
            ", assetFileTags=" + assetFileTags +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
