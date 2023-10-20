
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
import com.bytechef.atlas.workflow.mapper.WorkflowResource;

import java.util.Iterator;
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
        GitWorkflowRepository workflowRepository = new GitWorkflowRepository(new DummyGitWorkflowOperations());

        Iterable<Workflow> iterable = workflowRepository.findAll();

        Iterator<Workflow> iterator = iterable.iterator();

        Workflow workflow = iterator.next();

        Assertions.assertEquals("hello/123", workflow.getId());
    }

    private static class DummyGitWorkflowOperations implements GitWorkflowOperations {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        @Override
        public List<WorkflowResource> getHeadFiles() {
            return List.of(new WorkflowResource(
                "hello/123", resolver.getResource("classpath:workflows/hello.yaml"), Workflow.Format.YAML));
        }

        @Override
        public WorkflowResource getFile(String fileId) {
            return new WorkflowResource(
                "hello/123", resolver.getResource("classpath:workflows/hello.yaml"), Workflow.Format.YAML);
        }
    }
}
