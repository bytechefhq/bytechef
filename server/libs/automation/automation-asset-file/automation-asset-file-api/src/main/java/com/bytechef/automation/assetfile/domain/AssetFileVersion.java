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

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An immutable snapshot of an {@link AssetFile}'s previous content, captured every time the file's content is replaced.
 * The snapshot owns its blob: the version row points at the {@link FileEntry} the asset file pointed at before the
 * update, and restoring a version copies bytes into a fresh blob rather than sharing the pointer, so deleting either
 * row can safely delete its blob.
 *
 * @author Ivica Cardic
 */
@Table("asset_file_version")
public class AssetFileVersion {

    @Id
    private Long id;

    @Column("asset_file_id")
    private Long assetFileId;

    private int versionNumber;

    private FileEntry file;

    private String mimeType;

    private long sizeBytes;

    @CreatedDate
    private Instant createdDate;

    @CreatedBy
    private String createdBy;

    public AssetFileVersion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetFileId() {
        return assetFileId;
    }

    public void setAssetFileId(Long assetFileId) {
        this.assetFileId = assetFileId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public FileEntry getFile() {
        return file;
    }

    public void setFile(FileEntry file) {
        this.file = file;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AssetFileVersion that)) {
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
        return "AssetFileVersion{" +
            "id=" + id +
            ", assetFileId=" + assetFileId +
            ", versionNumber=" + versionNumber +
            ", file=" + file +
            ", mimeType='" + mimeType + '\'' +
            ", sizeBytes=" + sizeBytes +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            '}';
    }
}
