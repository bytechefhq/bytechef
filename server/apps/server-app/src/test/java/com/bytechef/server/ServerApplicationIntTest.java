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
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.ee.ai.copilot.web.rest.CopilotApiController;
import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableMigrator;
import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableProvisioner;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseTagFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
    PostgreSQLContainerConfiguration.class, ServerApplicationIntTest.ServerApplicationIntTestConfiguration.class
})
class ServerApplicationIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testContextLoads() {
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
    void testClickHouseContextStoreBeansAbsentWhenUrlNotConfigured() {
        // Phase 16: the ClickHouse module is on the runtime classpath, but with
        // bytechef.context-store.clickhouse.url unset the conditional cascade keeps every ClickHouse bean out of the
        // context. Router falls back to the Postgres adapter for all sources. Pins the "default deployment"
        // contract: pulling the module in doesn't impose any startup cost or side-effect for operators who haven't
        // opted in.
        assertThat(applicationContext.getBeanNamesForType(ClickHouseTableProvisioner.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(ClickHouseTableMigrator.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(javax.sql.DataSource.class))
            .doesNotContainKey("clickHouseDataSource");
        assertThat(applicationContext.containsBean("contextStoreRecordClickHouseRepository")).isFalse();
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

    @TestConfiguration
    static class ServerApplicationIntTestConfiguration {

        @Bean
        @Primary
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
