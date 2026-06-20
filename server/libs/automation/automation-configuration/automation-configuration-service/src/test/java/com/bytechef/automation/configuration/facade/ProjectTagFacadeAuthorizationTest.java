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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the project-tag gates. {@code updateProjectTags} keys on the project via {@code ProjectScope};
 * {@code getProjectTags} is workspace-scoped and gated by {@code WorkspaceRole VIEWER}.
 *
 * @author Ivica Cardic
 */
class ProjectTagFacadeAuthorizationTest {

    @Test
    void testUpdateProjectTagsRequiresWorkflowEdit() {
        Method method = findMethod("updateProjectTags", long.class, List.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on updateProjectTags")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission(#id, 'ProjectScope', 'WORKFLOW_EDIT')");
    }

    @Test
    void testGetProjectTagsRequiresWorkspaceViewer() {
        Method method = findMethod("getProjectTags", long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on getProjectTags")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')");
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return ProjectTagFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }
    }
}
