/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.workflow.repository.git;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.ee.atlas.configuration.repository.git.GitWorkflowRepository;
import com.bytechef.ee.atlas.configuration.repository.git.operations.GitWorkflowOperations;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @version ee
 *
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

        Assertions.assertEquals("aGVsbG8vMTIz", workflow.getId());
    }

    private static class DummyGitWorkflowOperations implements GitWorkflowOperations {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        @Override
        public List<WorkflowResource> getHeadFiles() {
            return List.of(
                new WorkflowResource(
                    "hello/123", Map.of(), resolver.getResource("classpath:workflows/hello.yaml"), Format.YAML));
        }

        @Override
        public WorkflowResource getFile(String fileId) {
            return new WorkflowResource(
                "hello/123", Map.of(), resolver.getResource("classpath:workflows/hello.yaml"), Format.YAML);
        }
    }
}
