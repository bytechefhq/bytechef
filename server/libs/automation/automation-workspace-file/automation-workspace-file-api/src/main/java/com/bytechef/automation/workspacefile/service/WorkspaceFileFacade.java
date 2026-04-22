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

package com.bytechef.automation.workspacefile.service;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import java.io.InputStream;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface WorkspaceFileFacade {

    WorkspaceFile createFromUpload(Long workspaceId, String filename, String contentType, InputStream data);

    WorkspaceFile createFromAi(
        Long workspaceId, String filename, String contentType, String content,
        Short generatedByAgentSource, String generatedFromPrompt);

    void delete(Long id);

    InputStream downloadContent(Long id);

    List<WorkspaceFile> findAllByWorkspaceId(Long workspaceId, List<Long> tagIds);

    WorkspaceFile findById(Long id);

    WorkspaceFile rename(Long id, String newName);

    WorkspaceFile updateContent(Long id, String contentType, InputStream data);

    WorkspaceFile updateTags(Long id, List<Long> tagIds);
}
