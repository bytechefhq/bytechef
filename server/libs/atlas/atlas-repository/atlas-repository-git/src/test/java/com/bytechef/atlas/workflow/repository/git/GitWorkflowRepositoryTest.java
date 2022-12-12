
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.workflow.repository.git;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.git.GitWorkflowRepository;
import com.bytechef.atlas.repository.git.workflow.GitWorkflowOperations;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowResource;
import com.bytechef.atlas.repository.workflow.mapper.YamlWorkflowMapper;
import com.bytechef.atlas.workflow.WorkflowFormat;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class GitWorkflowRepositoryTest {

    @Test
    public void test1() {
        GitWorkflowRepository workflowRepository = new GitWorkflowRepository(new DummyGitWorkflowOperations(),
            new YamlWorkflowMapper());

        Iterable<Workflow> findAll = workflowRepository.findAll();

        Assertions.assertEquals("hello/123", findAll.iterator()
            .next()
            .getId());
    }

    private static class DummyGitWorkflowOperations implements GitWorkflowOperations {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        @Override
        public List<WorkflowResource> getHeadFiles() {
            return List.of(new WorkflowResource(
                "hello/123", resolver.getResource("file:workflow/hello.yaml"), WorkflowFormat.YAML));
        }

        @Override
        public WorkflowResource getFile(String aFileId) {
            return new WorkflowResource(
                "hello/123", resolver.getResource("file:workflow/hello.yaml"), WorkflowFormat.YAML);
        }
    }
}
