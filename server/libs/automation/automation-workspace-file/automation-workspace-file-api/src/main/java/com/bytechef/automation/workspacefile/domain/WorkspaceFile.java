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

package com.bytechef.automation.workspacefile.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.file.storage.domain.FileEntry;
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
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("workspace_file")
public class WorkspaceFile {

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

    @MappedCollection(idColumn = "workspace_file_id")
    private Set<WorkspaceFileTag> workspaceFileTags = new HashSet<>();

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

    public WorkspaceFile() {
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

    public WorkspaceFileSource getSource() {
        return WorkspaceFileSource.valueOf(source);
    }

    public void setSource(WorkspaceFileSource source) {
        this.source = (short) source.ordinal();
    }

    public Short getGeneratedByAgentSource() {
        return generatedByAgentSource;
    }

    public void setGeneratedByAgentSource(Short generatedByAgentSource) {
        this.generatedByAgentSource = generatedByAgentSource;
    }

    public String getGeneratedFromPrompt() {
        return generatedFromPrompt;
    }

    public void setGeneratedFromPrompt(String generatedFromPrompt) {
        this.generatedFromPrompt = generatedFromPrompt;
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

    public List<Long> getTagIds() {
        return workspaceFileTags
            .stream()
            .map(WorkspaceFileTag::getTagId)
            .toList();
    }

    public void setTagIds(List<Long> tagIds) {
        this.workspaceFileTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (long tagId : tagIds) {
                workspaceFileTags.add(new WorkspaceFileTag(tagId));
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

        if (!(o instanceof WorkspaceFile that)) {
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
        return "WorkspaceFile{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", mimeType='" + mimeType + '\'' +
            ", sizeBytes=" + sizeBytes +
            ", file=" + file +
            ", source=" + source +
            ", generatedByAgentSource=" + generatedByAgentSource +
            ", generatedFromPrompt='" + generatedFromPrompt + '\'' +
            ", workspaceFileTags=" + workspaceFileTags +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
