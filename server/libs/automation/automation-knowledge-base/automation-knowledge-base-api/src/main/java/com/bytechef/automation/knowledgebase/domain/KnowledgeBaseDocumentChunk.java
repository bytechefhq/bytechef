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

package com.bytechef.automation.knowledgebase.domain;

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("knowledge_base_document_chunk")
public class KnowledgeBaseDocumentChunk {

    @Id
    private Long id;

    private Long knowledgeBaseDocumentId;

    private String vectorStoreId;

    private FileEntry content;

    @Transient
    private Map<String, ?> metadata;

    @Transient
    private Float score;

    @Transient
    private String textContent;

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

    public KnowledgeBaseDocumentChunk() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKnowledgeBaseDocumentId() {
        return knowledgeBaseDocumentId;
    }

    public void setKnowledgeBaseDocumentId(Long knowledgeBaseDocumentId) {
        this.knowledgeBaseDocumentId = knowledgeBaseDocumentId;
    }

    public String getVectorStoreId() {
        return vectorStoreId;
    }

    public void setVectorStoreId(String vectorStoreId) {
        this.vectorStoreId = vectorStoreId;
    }

    public FileEntry getContent() {
        return content;
    }

    public void setContent(FileEntry content) {
        this.content = content;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Map<String, ?> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, ?> metadata) {
        this.metadata = Collections.unmodifiableMap(metadata);
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof KnowledgeBaseDocumentChunk that)) {
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
        return "KnowledgeBaseDocumentChunk{" +
            "id=" + id +
            ", knowledgeBaseDocumentId=" + knowledgeBaseDocumentId +
            ", vectorStoreId='" + vectorStoreId + '\'' +
            ", content=" + content +
            ", textContent='" + textContent + '\'' +
            ", metadata=" + metadata +
            ", score=" + score +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
