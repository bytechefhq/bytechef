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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
class ProjectVisibilityFilterTest {

    @Test
    void testFilterKeepsOnlyResolvedIds() {
        ResourceVisibilityResolver resolver = (resourceType, workspaceId, candidates) -> {
            assertThat(resourceType).isEqualTo("Project");

            return Set.of(1L);
        };
        Project visible = project(1L);
        Project hidden = project(2L);

        ProjectVisibilityFilter filter = new ProjectVisibilityFilter(objectProvider(resolver));

        assertThat(filter.filterVisible(List.of(visible, hidden))).containsExactly(visible);
        assertThat(filter.visibleProjectIds(List.of())).isEmpty();
    }

    @Test
    void testAbsentResolverHidesEverythingRatherThanShowingEverything() {
        // The five distributed EE apps that carry automation-configuration-api without -service have no resolver
        // bean. Both projects are WORKSPACE-visible and would pass any resolver that ran, so a filter that fell
        // through to "unrestricted" would return both here.
        ProjectVisibilityFilter filter = new ProjectVisibilityFilter(objectProvider(null));

        assertThat(filter.visibleProjectIds(List.of(project(1L), project(2L)))).isEmpty();
        assertThat(filter.filterVisible(List.of(project(1L), project(2L)))).isEmpty();
    }

    @Test
    void testToVisibilityRecordCarriesReachAndOwner() {
        Project project = project(3L);

        project.setVisibility(ResourceVisibility.PRIVATE);

        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        assertThat(ProjectVisibilityFilter.toVisibilityRecord(project))
            .isEqualTo(new VisibilityRecord(3L, ResourceVisibility.PRIVATE, "ivica"));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceVisibilityResolver> objectProvider(ResourceVisibilityResolver resolver) {
        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resolver);

        return objectProvider;
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setVisibility(ResourceVisibility.WORKSPACE);

        return project;
    }
}
