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

package com.bytechef.automation.ai.a2a.security;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class A2aProjectWorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final A2aProjectRepository a2aProjectRepository;
    private final A2aProjectWorkflowRepository a2aProjectWorkflowRepository;
    private final A2aServerRepository a2aServerRepository;

    @SuppressFBWarnings("EI")
    public A2aProjectWorkflowOwnershipResolver(
        A2aProjectRepository a2aProjectRepository, A2aProjectWorkflowRepository a2aProjectWorkflowRepository,
        A2aServerRepository a2aServerRepository) {

        this.a2aProjectRepository = a2aProjectRepository;
        this.a2aProjectWorkflowRepository = a2aProjectWorkflowRepository;
        this.a2aServerRepository = a2aServerRepository;
    }

    @Override
    public String resourceType() {
        return "A2aProjectWorkflow";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return a2aProjectWorkflowRepository.findById(id)
            .map(A2aProjectWorkflow::getA2aProjectId)
            .flatMap(a2aProjectRepository::findById)
            .map(A2aProject::getA2aServerId)
            .flatMap(a2aServerRepository::findById)
            .map(A2aServer::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
