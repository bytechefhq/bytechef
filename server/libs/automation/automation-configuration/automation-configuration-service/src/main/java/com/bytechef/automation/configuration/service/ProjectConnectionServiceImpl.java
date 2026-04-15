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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.repository.ProjectConnectionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectConnectionServiceImpl implements ProjectConnectionService {

    private final ProjectConnectionRepository projectConnectionRepository;

    @SuppressFBWarnings("EI")
    public ProjectConnectionServiceImpl(ProjectConnectionRepository projectConnectionRepository) {
        this.projectConnectionRepository = projectConnectionRepository;
    }

    @Override
    public ProjectConnection create(long connectionId, long projectId) {
        return projectConnectionRepository.save(new ProjectConnection(connectionId, projectId));
    }

    @Override
    public void delete(long connectionId, long projectId) {
        projectConnectionRepository.deleteByConnectionIdAndProjectId(connectionId, projectId);
    }

    @Override
    public void deleteByConnectionId(long connectionId) {
        projectConnectionRepository.deleteAllByConnectionId(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConnection> getProjectConnections(long projectId) {
        return projectConnectionRepository.findAllByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConnection> getProjectConnectionsByProjectIds(List<Long> projectIds) {
        return projectConnectionRepository.findAllByProjectIdIn(projectIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConnection> getConnectionProjects(long connectionId) {
        return projectConnectionRepository.findAllByConnectionId(connectionId);
    }
}
