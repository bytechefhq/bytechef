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

package com.bytechef.automation.knowledgebase.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseStorageUsageGraphQlControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void testKnowledgeBaseStorageUsageReturnsFacadeValue() {
        WorkspaceKnowledgeBaseFacade facade = mock(WorkspaceKnowledgeBaseFacade.class);

        when(facade.getStorageUsage()).thenReturn(new KnowledgeBaseStorageUsage(800L, 1_000L, 80.0, false));

        KnowledgeBaseGraphQlController controller = new KnowledgeBaseGraphQlController(
            mock(ObjectProvider.class), mock(EnvironmentService.class),
            mock(KnowledgeBaseDocumentService.class), facade);

        KnowledgeBaseStorageUsage usage = controller.knowledgeBaseStorageUsage();

        assertThat(usage.percentage()).isEqualTo(80.0);
    }
}
