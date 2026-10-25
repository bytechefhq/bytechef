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

package com.bytechef.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.copilot.config.CopilotConfiguration;
import com.bytechef.ai.copilot.config.CopilotPgVectorConfiguration;
import com.bytechef.ai.copilot.service.CopilotVectorStoreService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.automation.configuration.web.rest.WorkflowApiController;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.ee.ai.copilot.web.rest.CopilotApiController;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.platform.coordinator.lock.SingleCoordinatorLock;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseTagFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.executor.WebhookWorkflowSyncExecutor;
import com.bytechef.platform.webhook.web.rest.ApprovalController;
import com.bytechef.platform.webhook.web.rest.CallbackController;
import com.bytechef.platform.webhook.web.rest.FileEntryController;
import com.bytechef.platform.webhook.web.rest.JobResumeController;
import com.bytechef.platform.webhook.web.rest.WebhookTriggerController;
import com.bytechef.platform.webhook.web.websocket.WebhookWebSocketConfiguration;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(properties = "bytechef.ai.vectorstore.pgvector.dimensions=768")
@Import({
    PostgreSQLContainerConfiguration.class, ServerApplicationIntTest.ServerApplicationIntTestConfiguration.class
})
class ServerApplicationIntTest {

    private static final String WORKFLOW_CACHE = "com.bytechef.atlas.configuration.service.WorkflowService.workflow";

    private static final Class<?>[] WEBHOOK_ENDPOINT_TYPES = {
        WebhookTriggerController.class, CallbackController.class, ApprovalController.class,
        JobResumeController.class, FileEntryController.class, WebhookWebSocketConfiguration.class,
        twilioCallbackControllerType()
    };

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private Scheduler scheduler;

    @Test
    void testContextLoads() {
    }

    @Test
    void testQuartzStartsUnclusteredWithDatabaseRowLocks() throws Exception {
        assertThat(scheduler.isStarted()).isTrue();
        assertThat(scheduler.getMetaData()
            .isJobStoreClustered()).isFalse();

        List<String> lockNames = new JdbcTemplate(dataSource).queryForList(
            "SELECT lock_name FROM qrtz_locks", String.class);

        assertThat(lockNames).contains("TRIGGER_ACCESS");
    }

    @Test
    void testCopilotBeansNotPresentWhenFeatureDisabled() {
        // Verify that Copilot beans are not present when bytechef.ai.copilot.enabled is false
        assertThat(applicationContext.getBeanNamesForType(CopilotConfiguration.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(CopilotVectorStoreService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(CopilotPgVectorConfiguration.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(CopilotApiController.class)).isEmpty();
    }

    @Test
    void testKnowledgeBaseBeansNotPresentWhenFeatureDisabled() {
        // Verify that knowledge base services are not present when the feature is disabled
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentChunkService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseTagFacade.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentTagService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(WorkspaceKnowledgeBaseService.class)).isEmpty();

        // Verify that knowledge base facades are not present
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseFacade.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentFacade.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentChunkFacade.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(WorkspaceKnowledgeBaseFacade.class)).isEmpty();
    }

    @Test
    void testPgVectorDimensionsFollowBytechefProperty() {
        assertThat(environment.getProperty("spring.ai.vectorstore.pgvector.dimensions", Integer.class))
            .isEqualTo(768);
    }

    @Test
    void testSpringBatchTablesAreCreated() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            assertThat(tableExists(databaseMetaData, "batch_job_instance")).isTrue();
            assertThat(tableExists(databaseMetaData, "batch_job_execution")).isTrue();
            assertThat(tableExists(databaseMetaData, "batch_step_execution")).isTrue();
        }
    }

    private static Class<?> twilioCallbackControllerType() {
        try {
            return Class.forName("com.bytechef.platform.webhook.web.rest.TwilioCallbackController");
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalStateException(classNotFoundException);
        }
    }

    private static boolean tableExists(DatabaseMetaData databaseMetaData, String tableName) throws Exception {
        try (ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, null)) {
            if (resultSet.next()) {
                return true;
            }
        }

        try (ResultSet resultSet = databaseMetaData.getTables(
            null, null, tableName.toUpperCase(Locale.ROOT), null)) {

            return resultSet.next();
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "bytechef.edition=ce", "spring.quartz.scheduler-name=communityEditionScheduler"
    })
    class CommunityEdition {

        @Autowired
        private ApplicationContext communityEditionApplicationContext;

        @Test
        void testContextLoadsInCommunityEdition() {
            assertThat(communityEditionApplicationContext.getBeanNamesForType(WorkspaceScopeCacheService.class))
                .isEmpty();
            assertThat(communityEditionApplicationContext.getBeanNamesForType(SingleCoordinatorLock.class))
                .isNotEmpty();
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "bytechef.coordinator.enabled=false", "bytechef.worker.enabled=false",
            "spring.quartz.scheduler-name=webhookNodeScheduler"
        })
    class WebhookNode {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private Scheduler scheduler;

        @Test
        void testSchedulerStaysInStandby() throws Exception {
            assertThat(scheduler.isStarted()).isFalse();
            assertThat(scheduler.isInStandbyMode()).isTrue();
        }

        @Test
        void testWebhookEndpointsArePresent() {
            for (Class<?> type : WEBHOOK_ENDPOINT_TYPES) {
                assertThat(applicationContext.getBeanNamesForType(type)).as(type.getSimpleName())
                    .isNotEmpty();
            }
        }

        @Test
        void testWebhookExecutionBeansArePresent() {
            for (Class<?> type : new Class<?>[] {
                WebhookWorkflowExecutor.class, WebhookWorkflowSyncExecutor.class, PrincipalJobFacade.class,
                TaskFileStorage.class, SseStreamBridgeRegistry.class, TaskHandlerRegistry.class
            }) {
                assertThat(applicationContext.getBeanNamesForType(type)).as(type.getSimpleName())
                    .isNotEmpty();
            }

            assertThat(applicationContext.containsBean("sseStreamMessageBrokerConfigurer")).isTrue();
        }

        @Test
        void testCoordinatorControllersAreAbsent() {
            assertThat(applicationContext.getBeanNamesForType(WorkflowApiController.class)).isEmpty();
        }

        @Test
        void testWorkflowsAreCached() {
            CacheManager cacheManager = applicationContext.getBean(CacheManager.class);

            assertThat(cacheManager.getCache(WORKFLOW_CACHE)).isNotInstanceOf(NoOpCache.class);
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "bytechef.edition=ce", "bytechef.coordinator.enabled=false", "bytechef.worker.enabled=false",
            "spring.quartz.scheduler-name=communityEditionWebhookNodeScheduler"
        })
    class CommunityEditionWebhookNode {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testWorkflowsAreNotCached() {
            CacheManager cacheManager = applicationContext.getBean(CacheManager.class);

            assertThat(cacheManager.getCache(WORKFLOW_CACHE)).isInstanceOf(NoOpCache.class);
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "bytechef.webhook.enabled=false", "spring.quartz.scheduler-name=noWebhookScheduler"
        })
    class CoordinatorWithoutWebhooks {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testWebhookEndpointsAreAbsent() {
            for (Class<?> type : WEBHOOK_ENDPOINT_TYPES) {
                assertThat(applicationContext.getBeanNamesForType(type)).as(type.getSimpleName())
                    .isEmpty();
            }

            assertThat(applicationContext.containsBean("sseStreamMessageBrokerConfigurer")).isFalse();
        }

        @Test
        void testCoordinatorControllersArePresent() {
            assertThat(applicationContext.getBeanNamesForType(WorkflowApiController.class)).isNotEmpty();
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "bytechef.scheduler.quartz.clustered=true", "bytechef.scheduler.quartz.cluster-checkin-interval=7000",
            "spring.quartz.scheduler-name=clusteredScheduler"
        })
    class ClusteredScheduler {

        @Autowired
        private Scheduler scheduler;

        @Test
        void testSchedulerIsClustered() throws Exception {
            assertThat(scheduler.getMetaData()
                .isJobStoreClustered()).isTrue();
        }
    }

    @TestConfiguration
    static class ServerApplicationIntTestConfiguration {

        @Bean
        @Primary
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
