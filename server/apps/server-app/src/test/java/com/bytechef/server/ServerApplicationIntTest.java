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

import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseTagService;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
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
@SpringBootTest
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
    void testKnowledgeBaseBeansNotPresentWhenFeatureDisabled() {
        // Verify that knowledge base services are not present when the feature is disabled
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseDocumentChunkService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(KnowledgeBaseTagService.class)).isEmpty();
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
