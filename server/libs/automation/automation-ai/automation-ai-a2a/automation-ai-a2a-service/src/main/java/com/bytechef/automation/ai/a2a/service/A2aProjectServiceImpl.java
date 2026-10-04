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

package com.bytechef.automation.ai.a2a.service;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aProjectServiceImpl implements A2aProjectService {

    private final A2aProjectRepository a2aProjectRepository;

    public A2aProjectServiceImpl(A2aProjectRepository a2aProjectRepository) {
        this.a2aProjectRepository = a2aProjectRepository;
    }

    @Override
    public A2aProject create(long projectDeploymentId, long a2aServerId, long projectId) {
        A2aProject a2aProject = new A2aProject(projectDeploymentId, a2aServerId, projectId);

        try {
            return a2aProjectRepository.save(a2aProject);
        } catch (RuntimeException runtimeException) {
            if (isDuplicateKey(runtimeException)) {
                throw new IllegalArgumentException(
                    "Project " + projectId + " is already attached to A2A server " + a2aServerId, runtimeException);
            }

            throw runtimeException;
        }
    }

    @Override
    public void delete(long a2aProjectId) {
        a2aProjectRepository.deleteById(a2aProjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<A2aProject> fetchA2aProject(long a2aProjectId) {
        return a2aProjectRepository.findById(a2aProjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProject> getA2aServerA2aProjects(long a2aServerId) {
        return a2aProjectRepository.findAllByA2aServerId(a2aServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProject> getProjectDeploymentA2aProjects(long projectDeploymentId) {
        return a2aProjectRepository.findAllByProjectDeploymentId(projectDeploymentId);
    }

    private static boolean isDuplicateKey(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof DuplicateKeyException) {
                return true;
            }
        }

        return false;
    }
}
