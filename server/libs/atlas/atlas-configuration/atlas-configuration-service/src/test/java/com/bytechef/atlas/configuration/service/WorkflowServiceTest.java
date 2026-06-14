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

package com.bytechef.atlas.configuration.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;

/**
 * Unit tests covering how {@link WorkflowServiceImpl#fetchWorkflow(String)} distinguishes a confirmed "not found" from
 * an inconclusive lookup (e.g. a transient datasource error). Reporting the latter as "not found" used to let callers
 * such as the trigger coordinator treat a still-live workflow as orphaned and delete its schedule (#5202).
 *
 * @author Ivica Cardic
 */
class WorkflowServiceTest {

    private static final String WORKFLOW_ID = "workflow-1";

    private CacheManager cacheManager;

    @BeforeEach
    void beforeEach() {
        Cache cache = mock(Cache.class);

        when(cache.get(anyString())).thenReturn(null);

        cacheManager = mock(CacheManager.class);

        when(cacheManager.getCache(anyString())).thenReturn(cache);
    }

    @Test
    void testFetchWorkflowRethrowsWhenRepositoryFailsAndWorkflowNotFound() {
        IllegalStateException failure = new IllegalStateException("datasource unavailable");

        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);

        when(workflowRepository.findById(WORKFLOW_ID)).thenThrow(failure);

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(workflowRepository));

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class, () -> workflowService.fetchWorkflow(WORKFLOW_ID));

        assertSame(failure, thrown);
    }

    @Test
    void testGetWorkflowRethrowsRepositoryFailureInsteadOfNotFound() {
        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);

        when(workflowRepository.findById(WORKFLOW_ID)).thenThrow(new IllegalStateException("datasource unavailable"));

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(workflowRepository));

        // A transient failure must not surface as ConfigurationException(WORKFLOW_NOT_FOUND).
        assertThrows(IllegalStateException.class, () -> workflowService.getWorkflow(WORKFLOW_ID));
    }

    @Test
    void testFetchWorkflowReturnsEmptyWhenAllRepositoriesReportNotFound() {
        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);

        when(workflowRepository.findById(WORKFLOW_ID)).thenReturn(Optional.empty());

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(workflowRepository));

        assertFalse(workflowService.fetchWorkflow(WORKFLOW_ID)
            .isPresent());
    }

    @Test
    void testGetWorkflowThrowsConfigurationExceptionWhenGenuinelyMissing() {
        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);

        when(workflowRepository.findById(WORKFLOW_ID)).thenReturn(Optional.empty());

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(workflowRepository));

        assertThrows(ConfigurationException.class, () -> workflowService.getWorkflow(WORKFLOW_ID));
    }

    @Test
    void testFetchWorkflowFallsBackToLaterRepositoryWhenEarlierThrows() {
        Workflow workflow = new Workflow("{\"tasks\": []}", Format.JSON);

        WorkflowRepository failingRepository = mock(WorkflowRepository.class);
        WorkflowRepository servingRepository = mock(WorkflowRepository.class);

        when(failingRepository.findById(WORKFLOW_ID)).thenThrow(new IllegalStateException("datasource unavailable"));
        when(servingRepository.findById(WORKFLOW_ID)).thenReturn(Optional.of(workflow));
        when(servingRepository.getSourceType()).thenReturn(SourceType.JDBC);

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(failingRepository, servingRepository));

        Optional<Workflow> result = workflowService.fetchWorkflow(WORKFLOW_ID);

        assertTrue(result.isPresent());
        assertSame(workflow, result.get());
    }

    @Test
    void testFetchWorkflowRethrowsLastFailureWhenAllRepositoriesFail() {
        IllegalStateException firstFailure = new IllegalStateException("first datasource unavailable");
        IllegalStateException secondFailure = new IllegalStateException("second datasource unavailable");

        WorkflowRepository firstRepository = mock(WorkflowRepository.class);
        WorkflowRepository secondRepository = mock(WorkflowRepository.class);

        when(firstRepository.findById(WORKFLOW_ID)).thenThrow(firstFailure);
        when(secondRepository.findById(WORKFLOW_ID)).thenThrow(secondFailure);

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(firstRepository, secondRepository));

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class, () -> workflowService.fetchWorkflow(WORKFLOW_ID));

        assertSame(secondFailure, thrown);
    }

    @Test
    void testFetchWorkflowReturnsCachedWorkflowWithoutQueryingRepositories() {
        Workflow workflow = new Workflow("{\"tasks\": []}", Format.JSON);

        Cache cache = mock(Cache.class);

        when(cache.get(anyString())).thenReturn(new SimpleValueWrapper(workflow));

        when(cacheManager.getCache(anyString())).thenReturn(cache);

        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);

        WorkflowServiceImpl workflowService = new WorkflowServiceImpl(
            cacheManager, List.of(), List.of(workflowRepository));

        Optional<Workflow> result = workflowService.fetchWorkflow(WORKFLOW_ID);

        assertTrue(result.isPresent());
        assertSame(workflow, result.get());

        verify(workflowRepository, never()).findById(anyString());
    }
}
