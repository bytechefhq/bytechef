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

package com.bytechef.automation.configuration.ownership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OwnerContributorTest {

    @Test
    void testProjectOwnerContributorFormatsTheColumnIntoTheTemplate() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM catalog WHERE catalog.project_id = %1$s OR catalog.parent_id = %1$s)");

        assertThat(projectOwnerContributor.getProjectOwner()).isEqualTo(ProjectOwner.EMBEDDED_CATALOG);
        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo(
                "EXISTS (SELECT 1 FROM catalog WHERE catalog.project_id = project.id OR " +
                    "catalog.parent_id = project.id)");
    }

    @Test
    void testProjectDeploymentOwnerContributorFormatsTheColumnIntoTheTemplate() {
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.MCP_SERVER,
            "EXISTS (SELECT 1 FROM mcp_project WHERE mcp_project.project_deployment_id = %1$s)");

        assertThat(projectDeploymentOwnerContributor.getProjectDeploymentOwner())
            .isEqualTo(ProjectDeploymentOwner.MCP_SERVER);
        assertThat(projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("project_deployment.id"))
            .isEqualTo("EXISTS (SELECT 1 FROM mcp_project WHERE mcp_project.project_deployment_id = " +
                "project_deployment.id)");
    }

    @Test
    void testFactoriesRejectAPositionalPlaceholder() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = ?"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s = ?"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testFactoriesRejectANamedPlaceholder() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = :projectId"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s IN (:ids)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testFactoriesAcceptACast() {
        assertThatCode(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s::text = '1'"))
            .doesNotThrowAnyException();
        assertThatCode(
            () -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s::text = '1'"))
                .doesNotThrowAnyException();
    }

    @Test
    void testFactoriesRejectATemplateWithoutTheIdColumn() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "1 = 1"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "1 = 1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testFactoriesRejectATemplateWithAStrayPercentSign() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND name LIKE 'a%'"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(
                ProjectDeploymentOwner.EMBEDDED, "%1$s = 1 AND name LIKE '%a'"))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testContributorsRejectAnInvalidIdColumn() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1");
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED, "%1$s = 1");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id OR 1 = 1"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("a.b.c"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("1id"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testContributorsAcceptAnUnqualifiedAndAQualifiedIdColumn() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1");

        assertThat(projectOwnerContributor.getProjectClaimCondition("id")).isEqualTo("id = 1");
        assertThat(projectOwnerContributor.getProjectClaimCondition("project_deployment.project_id"))
            .isEqualTo("project_deployment.project_id = 1");
    }
}
