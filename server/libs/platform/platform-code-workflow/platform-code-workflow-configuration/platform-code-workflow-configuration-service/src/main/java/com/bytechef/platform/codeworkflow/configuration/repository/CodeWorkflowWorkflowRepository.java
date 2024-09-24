/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.codeworkflow.configuration.repository;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CodeWorkflowWorkflowRepository implements WorkflowCrudRepository {

    private final CodeWorkflowContainerRepository codeWorkflowContainerRepository;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;

    @SuppressFBWarnings("EI")
    public CodeWorkflowWorkflowRepository(
        CodeWorkflowContainerRepository codeWorkflowContainerRepository,
        CodeWorkflowFileStorage codeWorkflowFileStorage) {

        this.codeWorkflowContainerRepository = codeWorkflowContainerRepository;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
    }

    @Override
    public List<Workflow> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Workflow> findById(String id) {
        return codeWorkflowContainerRepository.findByWorkflowId(id)
            .map(codeWorkflowContainer -> toWorkflow(codeWorkflowContainer, id));
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.JDBC;
    }

    @Override
    public void deleteById(String id) {
    }

    @Override
    public Workflow save(Workflow workflow) {
        throw new UnsupportedOperationException();
    }

    private Workflow toWorkflow(CodeWorkflowContainer codeWorkflowContainer, String workflowId) {
        return new Workflow(
            workflowId,
            codeWorkflowFileStorage.readCodeWorkflowDefinition(
                codeWorkflowContainer.getDefinition(workflowId)),
            Format.JSON, codeWorkflowContainer.getLastModifiedDate(), Map.of());
    }
}
