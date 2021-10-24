/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.workflow.repository.git;

import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowFormatType;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowResource;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import java.util.Arrays;
import java.util.List;

import com.integri.atlas.engine.coordinator.workflow.repository.YAMLWorkflowMapper;
import com.integri.atlas.repository.workflow.git.GitOperations;
import com.integri.atlas.repository.workflow.git.GitWorkflowRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class GitWorkflowRepositoryTest {

    @Test
    public void test1() {
        GitWorkflowRepository r = new GitWorkflowRepository(new DummyGitOperations(), new YAMLWorkflowMapper());
        List<Workflow> findAll = r.findAll();
        Assertions.assertEquals("demo/hello/123", findAll.iterator().next().getId());
    }

    private static class DummyGitOperations implements GitOperations {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        @Override
        public List<WorkflowResource> getHeadFiles() {
            return Arrays.asList(
                new WorkflowResource(
                    "demo/hello/123", resolver.getResource("file:workflows/demo/hello.yaml"),
                    WorkflowFormatType.YAML)
            );
        }

        @Override
        public WorkflowResource getFile(String aFileId) {
            return new WorkflowResource(
                "demo/hello/123", resolver.getResource("file:workflows/demo/hello.yaml"), WorkflowFormatType.YAML);
        }
    }
}
