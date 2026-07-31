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

package com.bytechef.automation.assetfile.cleanup;

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A blob whose delete failed and is queued for retry by {@code AssetFileOrphanBlobCleaner}. Rows are written at the
 * exact points where a blob delete fails (after-commit deletes, rollback cleanup, version pruning) so the cleaner never
 * needs to enumerate storage — the queue IS the orphan set.
 *
 * @author Ivica Cardic
 */
@Table("asset_file_orphan_blob")
public class AssetFileOrphanBlob {

    @Id
    private Long id;

    private FileEntry file;

    private int attempts;

    private Instant lastAttemptDate;

    @CreatedDate
    private Instant createdDate;

    public AssetFileOrphanBlob() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileEntry getFile() {
        return file;
    }

    public void setFile(FileEntry file) {
        this.file = file;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getLastAttemptDate() {
        return lastAttemptDate;
    }

    public void setLastAttemptDate(Instant lastAttemptDate) {
        this.lastAttemptDate = lastAttemptDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AssetFileOrphanBlob that)) {
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
        return "AssetFileOrphanBlob{" +
            "id=" + id +
            ", file=" + file +
            ", attempts=" + attempts +
            ", lastAttemptDate=" + lastAttemptDate +
            ", createdDate=" + createdDate +
            '}';
    }
}
