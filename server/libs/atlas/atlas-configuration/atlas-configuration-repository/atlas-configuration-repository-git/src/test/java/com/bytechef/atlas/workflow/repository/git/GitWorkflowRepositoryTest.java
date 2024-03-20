/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.workflow.repository.git;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.MapUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class GitWorkflowRepositoryTest {

    @BeforeAll
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void beforeAll() {
        class MapUtilsMock extends MapUtils {
            static {
                objectMapper = new ObjectMapper();
            }
        }

        new MapUtilsMock();
    }

    @Test
    public void test1() {
        GitWorkflowRepository workflowRepository = new GitWorkflowRepository(0, new DummyGitWorkflowOperations());

        Iterable<Workflow> iterable = workflowRepository.findAll(0);

        Iterator<Workflow> iterator = iterable.iterator();

        Workflow workflow = iterator.next();

        Assertions.assertEquals("aGVsbG8vMTIz", workflow.getId());
    }

    private static class DummyGitWorkflowOperations implements GitWorkflowOperations {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        @Override
        public List<WorkflowResource> getHeadFiles() {
            return List.of(new WorkflowResource(
                "hello/123", Map.of(), resolver.getResource("classpath:workflows/hello.yaml"), Format.YAML));
        }

        @Override
        public WorkflowResource getFile(String fileId) {
            return new WorkflowResource(
                "hello/123", Map.of(), resolver.getResource("classpath:workflows/hello.yaml"), Format.YAML);
        }
    }
}
