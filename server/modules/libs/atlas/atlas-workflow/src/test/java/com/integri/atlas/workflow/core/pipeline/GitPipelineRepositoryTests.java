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

package com.integri.atlas.workflow.core.pipeline;

import com.integri.atlas.workflow.core.git.GitOperations;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class GitPipelineRepositoryTests {

    @Test
    public void test1() {
        GitPipelineRepository r = new GitPipelineRepository(new DummyGitOperations());
        List<Pipeline> findAll = r.findAll();
        Assertions.assertEquals("demo/hello/123", findAll.iterator().next().getId());
    }

    private static class DummyGitOperations implements GitOperations {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        @Override
        public List<IdentifiableResource> getHeadFiles() {
            return Arrays.asList(
                new IdentifiableResource("demo/hello/123", resolver.getResource("file:pipelines/demo/hello.yaml"))
            );
        }

        @Override
        public IdentifiableResource getFile(String aFileId) {
            return new IdentifiableResource("demo/hello/123", resolver.getResource("file:pipelines/demo/hello.yaml"));
        }
    }
}
