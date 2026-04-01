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

package com.bytechef.ai.mcp.tool.automation.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

/**
 * @author Marko Kriskovic
 */
@ExtendWith(MockitoExtension.class)
class ProjectWorkflowToolsImplTest {

    @Mock
    private com.bytechef.platform.workflow.validator.WorkflowValidatorFacadeImpl workflowValidatorFacade;

    @Test
    void instantiatesSuccessfully() throws IOException {
        Resource emptyResource = mock(Resource.class);

        when(emptyResource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        new ProjectWorkflowToolsImpl(null, workflowValidatorFacade, emptyResource, emptyResource, emptyResource);
    }
}
