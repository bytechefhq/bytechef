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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectVisibilityPolicyTest {

    private final ResourceVisibilityPolicyRegistry registry =
        new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy()));

    @Test
    void testDefaultIsWorkspace() {
        assertThat(registry.defaultVisibility("Project")).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testTheEntityFieldDefaultAgreesWithThePolicyDefault() {
        Project project = new Project();

        assertThat(project.getVisibility()).isEqualTo(registry.defaultVisibility("Project"));
    }

    @Test
    void testOrganizationIsNotSupported() {
        assertThat(registry.supports("Project", ResourceVisibility.PRIVATE)).isTrue();
        assertThat(registry.supports("Project", ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(registry.supports("Project", ResourceVisibility.ORGANIZATION))
            .as("a project belongs to one workspace; there is no representation for it outside that workspace")
            .isFalse();
    }
}
