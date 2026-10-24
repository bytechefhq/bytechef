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

package com.bytechef.automation.datasync.service;

import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import com.bytechef.automation.datasync.repository.DataSyncRepository.ProjectWorkflowProject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link DataSyncService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class DataSyncServiceImpl implements DataSyncService {

    private final DataSyncRepository dataSyncRepository;

    public DataSyncServiceImpl(DataSyncRepository dataSyncRepository) {
        this.dataSyncRepository = dataSyncRepository;
    }

    @Override
    public DataSync create(DataSync dataSync) {
        return dataSyncRepository.save(dataSync);
    }

    @Override
    public void delete(long id) {
        dataSyncRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataSync> fetchDataSync(long id) {
        return dataSyncRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DataSync getDataSync(long id) {
        return dataSyncRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DataSync with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSync> getDataSyncs(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return dataSyncRepository.findAll();
        }

        return dataSyncRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getProjectId(DataSync dataSync) {
        return dataSyncRepository.findProjectIdByProjectWorkflowUuid(dataSync.getProjectWorkflowUuid())
            .orElseThrow(() -> new IllegalStateException(
                "Data Sync %d has no project workflow".formatted(dataSync.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getProjectIds(Collection<DataSync> dataSyncs) {
        if (dataSyncs.isEmpty()) {
            return Map.of();
        }

        Set<UUID> projectWorkflowUuids = dataSyncs.stream()
            .map(DataSync::getProjectWorkflowUuid)
            .collect(Collectors.toSet());

        List<ProjectWorkflowProject> projectWorkflowProjects =
            dataSyncRepository.findProjectIdsByProjectWorkflowUuids(projectWorkflowUuids);

        Map<UUID, Long> projectIdsByUuid = new HashMap<>();

        for (ProjectWorkflowProject projectWorkflowProject : projectWorkflowProjects) {
            projectIdsByUuid.put(projectWorkflowProject.projectWorkflowUuid(), projectWorkflowProject.projectId());
        }

        Map<Long, Long> projectIds = new HashMap<>();

        for (DataSync dataSync : dataSyncs) {
            Long projectId = projectIdsByUuid.get(dataSync.getProjectWorkflowUuid());

            if (projectId != null) {
                projectIds.put(dataSync.getId(), projectId);
            }
        }

        return projectIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSync> getProjectDataSyncs(long projectId) {
        return dataSyncRepository.findAllByProjectId(projectId);
    }

    @Override
    public DataSync update(DataSync dataSync) {
        DataSync currentDataSync = getDataSync(dataSync.getId());

        currentDataSync.setName(dataSync.getName());
        currentDataSync.setTitle(dataSync.getTitle());
        currentDataSync.setDescription(dataSync.getDescription());
        currentDataSync.setWorkspaceId(dataSync.getWorkspaceId());
        currentDataSync.setTriggerType(dataSync.getTriggerType());
        currentDataSync.setTriggerParameters(dataSync.getTriggerParameters());
        currentDataSync.setVersion(dataSync.getVersion());

        return dataSyncRepository.save(currentDataSync);
    }
}
