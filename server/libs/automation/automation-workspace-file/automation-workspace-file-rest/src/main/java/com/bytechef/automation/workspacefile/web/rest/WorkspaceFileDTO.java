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

package com.bytechef.automation.workspacefile.web.rest;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import java.time.Instant;

/**
 * @author Ivica Cardic
 */
public record WorkspaceFileDTO(
    Long id,
    String name,
    String description,
    String mimeType,
    long sizeBytes,
    String source,
    String createdBy,
    Instant createdDate,
    String lastModifiedBy,
    Instant lastModifiedDate) {

    public static WorkspaceFileDTO from(WorkspaceFile workspaceFile) {
        return new WorkspaceFileDTO(
            workspaceFile.getId(),
            workspaceFile.getName(),
            workspaceFile.getDescription(),
            workspaceFile.getMimeType(),
            workspaceFile.getSizeBytes(),
            workspaceFile.getSource()
                .name(),
            workspaceFile.getCreatedBy(),
            workspaceFile.getCreatedDate(),
            workspaceFile.getLastModifiedBy(),
            workspaceFile.getLastModifiedDate());
    }
}
