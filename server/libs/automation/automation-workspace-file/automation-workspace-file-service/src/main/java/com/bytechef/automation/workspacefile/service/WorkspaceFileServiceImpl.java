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
import com.bytechef.automation.workspacefile.domain.WorkspaceWorkspaceFile;
import com.bytechef.automation.workspacefile.repository.WorkspaceFileRepository;
import com.bytechef.automation.workspacefile.repository.WorkspaceWorkspaceFileRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceFileServiceImpl implements WorkspaceFileService {

    private final WorkspaceFileRepository workspaceFileRepository;
    private final WorkspaceWorkspaceFileRepository workspaceWorkspaceFileRepository;

    public WorkspaceFileServiceImpl(
        WorkspaceFileRepository workspaceFileRepository,
        WorkspaceWorkspaceFileRepository workspaceWorkspaceFileRepository) {

        this.workspaceFileRepository = workspaceFileRepository;
        this.workspaceWorkspaceFileRepository = workspaceWorkspaceFileRepository;
    }

    @Override
    public WorkspaceFile create(WorkspaceFile workspaceFile, Long workspaceId) {
        Assert.notNull(workspaceId, "workspaceId is required");

        WorkspaceFile savedWorkspaceFile = workspaceFileRepository.save(workspaceFile);

        workspaceWorkspaceFileRepository.save(new WorkspaceWorkspaceFile(savedWorkspaceFile.getId(), workspaceId));

        return savedWorkspaceFile;
    }

    @Override
    public void delete(Long id) {
        workspaceFileRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceFile> fetchByWorkspaceIdAndName(Long workspaceId, String name) {
        return workspaceFileRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .filter(workspaceFile -> Objects.equals(workspaceFile.getName(), name))
            .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceFile> findAllByWorkspaceId(Long workspaceId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return workspaceFileRepository.findAllByWorkspaceId(workspaceId);
        }

        return workspaceFileRepository.findAllByWorkspaceIdAndTagIdsIn(workspaceId, tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceFile findById(Long id) {
        return workspaceFileRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("WorkspaceFile %d not found".formatted(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public long sumSizeBytesByWorkspaceId(Long workspaceId) {
        return workspaceFileRepository.sumSizeBytesByWorkspaceId(workspaceId);
    }

    @Override
    public WorkspaceFile update(WorkspaceFile workspaceFile) {
        return workspaceFileRepository.save(workspaceFile);
    }
}
