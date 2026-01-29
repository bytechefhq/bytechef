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

package com.bytechef.configuration;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ConfigurationApplication.class)
@Import({
    PostgreSQLContainerConfiguration.class,
})
@MockitoBean(types = {
    DataTableService.class, KnowledgeBaseFacade.class, KnowledgeBaseDocumentChunkFacade.class,
    KnowledgeBaseDocumentFacade.class, WorkspaceKnowledgeBaseFacade.class, KnowledgeBaseDocumentService.class,
    KnowledgeBaseService.class, WorkspaceKnowledgeBaseService.class
})
public class ConfigurationApplicationIntTest {

    @Test
    void testContextLoads() {
    }
}
