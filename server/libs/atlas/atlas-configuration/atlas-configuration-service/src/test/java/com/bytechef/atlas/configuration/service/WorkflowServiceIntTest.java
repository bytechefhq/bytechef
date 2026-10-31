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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Runs {@link WorkflowServiceImpl} over the real JDBC workflow repository. A Mockito-backed {@link WorkflowRepository}
 * ordered ahead of the JDBC one (the "probing" repository) stands in for another workflow source that can fail, and the
 * JDBC repository is a {@link MockitoSpyBean} so a datasource failure can be injected where Postgres cannot produce one
 * on demand (#5202).
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class WorkflowServiceIntTest {

    private static final String WORKFLOW_CACHE = WorkflowService.class.getName() + ".workflow";
    private static final String WORKFLOWS_CACHE = WorkflowService.class.getName() + ".workflows";

    @Autowired
    private RecordingCacheManager cacheManager;

    @MockitoSpyBean
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private WorkflowRepository probingWorkflowRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    public void beforeEach() {
        reset(probingWorkflowRepository);

        cacheManager.clear();
    }

    @Test
    public void testCreate() {
        Workflow workflow = getWorkflow();

        workflow = workflowService.create(
            workflow.getDefinition(), workflow.getFormat(), Workflow.SourceType.JDBC);

        Assertions.assertEquals(workflow, OptionalUtils.get(workflowCrudRepository.findById(workflow.getId())));
    }

    @Test
    public void testDelete() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        workflowService.delete(Validate.notNull(workflow.getId(), "id"));

        Assertions.assertFalse(OptionalUtils.isPresent(workflowCrudRepository.findById(workflow.getId())));
    }

    @Test
    public void testGetWorkflow() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Assertions.assertEquals(workflow, workflowService.getWorkflow(Validate.notNull(workflow.getId(), "id")));
    }

    @Test
    public void testUpdate() {
        String definition = "{\"label\": \"Label\",\"tasks\": []}";
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Workflow updatedWorkflow = workflowService.update(
            Validate.notNull(workflow.getId(), "id"), definition, workflow.getVersion());

        Assertions.assertEquals(definition, updatedWorkflow.getDefinition());
    }

    @Test
    public void testFetchWorkflowRethrowsWhenRepositoryFailsAndWorkflowNotFound() {
        String workflowId = "missing-" + System.nanoTime();
        IllegalStateException failure = new IllegalStateException("datasource unavailable");

        doThrow(failure).when(probingWorkflowRepository)
            .findById(workflowId);

        assertThatThrownBy(() -> workflowService.fetchWorkflow(workflowId))
            .isSameAs(failure);
    }

    @Test
    public void testGetWorkflowRethrowsRepositoryFailureInsteadOfNotFound() {
        String workflowId = "missing-" + System.nanoTime();

        doThrow(new IllegalStateException("datasource unavailable")).when(probingWorkflowRepository)
            .findById(workflowId);

        assertThatThrownBy(() -> workflowService.getWorkflow(workflowId))
            .as("a transient failure must not surface as ConfigurationException(WORKFLOW_NOT_FOUND)")
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testFetchWorkflowReturnsEmptyWhenAllRepositoriesReportNotFound() {
        assertThat(workflowService.fetchWorkflow("missing-" + System.nanoTime())).isEmpty();
    }

    @Test
    public void testGetWorkflowThrowsConfigurationExceptionWhenGenuinelyMissing() {
        String workflowId = "missing-" + System.nanoTime();

        assertThatThrownBy(() -> workflowService.getWorkflow(workflowId))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    public void testFetchWorkflowFallsBackToLaterRepositoryWhenEarlierThrows() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        String workflowId = Validate.notNull(workflow.getId(), "id");

        doThrow(new IllegalStateException("datasource unavailable")).when(probingWorkflowRepository)
            .findById(workflowId);

        Optional<Workflow> result = workflowService.fetchWorkflow(workflowId);

        assertThat(result).hasValueSatisfying(fetchedWorkflow -> {
            assertThat(fetchedWorkflow.getId()).isEqualTo(workflowId);
            assertThat(fetchedWorkflow.getDefinition()).isEqualTo(workflow.getDefinition());
            assertThat(fetchedWorkflow.getSourceType()).isEqualTo(Workflow.SourceType.JDBC);
        });
    }

    @Test
    public void testFetchWorkflowRethrowsLastFailureWhenAllRepositoriesFail() {
        String workflowId = "missing-" + System.nanoTime();
        IllegalStateException firstFailure = new IllegalStateException("first datasource unavailable");
        IllegalStateException secondFailure = new IllegalStateException("second datasource unavailable");

        doThrow(firstFailure).when(probingWorkflowRepository)
            .findById(workflowId);
        WorkflowRepository jdbcRepository = jdbcWorkflowRepository;

        doThrow(secondFailure).when(jdbcRepository)
            .findById(workflowId);

        assertThatThrownBy(() -> workflowService.fetchWorkflow(workflowId))
            .isSameAs(secondFailure);
    }

    @Test
    public void testFetchWorkflowReturnsCachedWorkflowWithoutQueryingRepositories() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        String workflowId = Validate.notNull(workflow.getId(), "id");

        assertThat(workflowService.fetchWorkflow(workflowId)).isPresent();

        workflowCrudRepository.deleteById(workflowId);

        clearInvocations(probingWorkflowRepository, jdbcWorkflowRepository);

        Optional<Workflow> result = workflowService.fetchWorkflow(workflowId);

        assertThat(result).hasValueSatisfying(
            cachedWorkflow -> assertThat(cachedWorkflow.getId()).isEqualTo(workflowId));

        verify(probingWorkflowRepository, never()).findById(anyString());
        WorkflowRepository jdbcRepository = jdbcWorkflowRepository;

        verify(jdbcRepository, never()).findById(anyString());
    }

    @Test
    public void testUpdateEvictsWorkflowEntriesNotCachedLocally() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        String workflowId = Validate.notNull(workflow.getId(), "id");

        workflowService.update(workflowId, "{\"label\": \"Updated\",\"tasks\": []}", workflow.getVersion());

        assertWorkflowEntriesEvicted(workflowId);

        Workflow persistedWorkflow = workflowCrudRepository.findById(workflowId)
            .orElseThrow();

        assertThat(persistedWorkflow.getDefinition()).isEqualTo("{\"label\": \"Updated\",\"tasks\": []}");
    }

    @Test
    public void testDeleteEvictsWorkflowEntries() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        String workflowId = Validate.notNull(workflow.getId(), "id");

        workflowService.delete(workflowId);

        assertWorkflowEntriesEvicted(workflowId);

        assertThat(workflowCrudRepository.findById(workflowId)).isEmpty();
    }

    private void assertWorkflowEntriesEvicted(String workflowId) {
        assertThat(cacheManager.getEvictedKeys(WORKFLOW_CACHE)).contains(TenantCacheKeyUtils.getKey(workflowId));
        assertThat(cacheManager.getEvictedKeys(WORKFLOWS_CACHE)).contains(TenantCacheKeyUtils.getKey(WORKFLOWS_CACHE));
    }

    private static Workflow getWorkflow() {
        Workflow workflow = new Workflow("{\"tasks\": []}", Workflow.Format.JSON);

        workflow.setNew(true);

        return workflow;
    }

    /**
     * A real in-memory cache manager that also records evicted keys, so eviction of entries that are absent from the
     * local cache (as with a distributed cache) stays observable.
     */
    public static class RecordingCacheManager extends ConcurrentMapCacheManager {

        private final ConcurrentHashMap<String, Set<Object>> evictedKeys = new ConcurrentHashMap<>();

        public void clear() {
            for (String cacheName : getCacheNames()) {
                Cache cache = Validate.notNull(getCache(cacheName), cacheName);

                cache.clear();
            }

            evictedKeys.clear();
        }

        public Set<Object> getEvictedKeys(String cacheName) {
            return evictedKeys.getOrDefault(cacheName, Set.of());
        }

        @Override
        protected Cache createConcurrentMapCache(String name) {
            return new ConcurrentMapCache(name) {

                @Override
                public void evict(Object key) {
                    evictedKeys.computeIfAbsent(name, cacheName -> ConcurrentHashMap.newKeySet())
                        .add(key);

                    super.evict(key);
                }
            };
        }
    }

    @EnableAutoConfiguration
    @Configuration
    public static class WorkflowConfigurationIntTestConfiguration {

        @Bean
        RecordingCacheManager cacheManager() {
            return new RecordingCacheManager();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        WorkflowRepository probingWorkflowRepository() {
            return mock(WorkflowRepository.class);
        }

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        public static class WorkflowIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
