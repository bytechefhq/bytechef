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

package com.bytechef.automation.configuration.web.graphql;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectGraphQlControllerErrorWorkflowTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProjectFacade projectFacade;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectGraphQlController projectGraphQlController;

    @Test
    void testUpdateProjectErrorWorkflowDelegatesToTheFacade() {
        Boolean result = projectGraphQlController.updateProjectErrorWorkflow(1L, 99L);

        assertTrue(result);

        verify(projectFacade).updateProjectErrorWorkflow(1L, 99L);
    }

    @Test
    void testUpdateProjectErrorWorkflowWrapsValidationFailureAsGraphQlBadRequest() {
        Mockito.doThrow(new IllegalArgumentException("The error workflow must belong to the same project"))
            .when(projectFacade)
            .updateProjectErrorWorkflow(1L, 99L);

        GraphQlBadRequestException exception = Assertions.assertThrows(
            GraphQlBadRequestException.class,
            () -> projectGraphQlController.updateProjectErrorWorkflow(1L, 99L));

        Assertions.assertEquals("The error workflow must belong to the same project", exception.getMessage());
    }
}
