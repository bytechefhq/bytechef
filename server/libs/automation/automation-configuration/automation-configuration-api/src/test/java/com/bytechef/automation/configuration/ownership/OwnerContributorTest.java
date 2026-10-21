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

import java.util.ArrayList;
import java.util.List;
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
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not contain placeholders");
        assertThatThrownBy(() -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s = ?"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not contain placeholders");
    }

    @Test
    void testFactoriesRejectANamedPlaceholder() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = :projectId"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not contain placeholders");
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s IN (:ids)"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain placeholders");
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
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("outside string literals");
        assertThatThrownBy(() -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "1 = 1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("outside string literals");
    }

    @Test
    void testFactoriesRejectATemplateThatOnlyReferencesTheIdColumnInsideAStringLiteral() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "'%1$s' IS NOT NULL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("outside string literals");
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "'%1$s' = '%1$s'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside string literals");
    }

    @Test
    void testContributorsRejectASubqueryThatShadowsTheIdColumnTable() {
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED,
            "EXISTS (SELECT 1 FROM project_deployment WHERE project_deployment.id = %1$s)");
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM catalog, project WHERE project.id = %1$s)");

        assertThatThrownBy(
            () -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("project_deployment.id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must alias the");
        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsRejectASchemaQualifiedSubqueryThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM public.project WHERE project.id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsRejectASchemaQualifiedOnlyTableThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM ONLY public.project WHERE project.id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsRejectAParenthesizedSchemaQualifiedJoinThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM (public.project CROSS JOIN catalog) WHERE project.id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsAcceptASchemaQualifiedOnlyTableThatAliasesTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM ONLY public.project owned_project WHERE owned_project.id = %1$s)");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo(
                "EXISTS (SELECT 1 FROM ONLY public.project owned_project WHERE owned_project.id = project.id)");
    }

    @Test
    void testContributorsAcceptAParenthesizedColumnNamedLikeTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM catalog WHERE (catalog.project = %1$s))");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("EXISTS (SELECT 1 FROM catalog WHERE (catalog.project = project.id))");
    }

    @Test
    void testContributorsRejectAnUnaliasedJoinThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM catalog JOIN project ON project.catalog_id = catalog.id WHERE project.id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsRejectAnAliasThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM catalog project WHERE project.id = %1$s)");
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED,
            "EXISTS (SELECT 1 FROM mcp_project AS project_deployment WHERE project_deployment.id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
        assertThatThrownBy(
            () -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("project_deployment.id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsRejectALockingClauseAfterAnUnaliasedIdColumnTable() {
        for (String lockingClause : List.of("FOR UPDATE", "FOR SHARE", "FOR KEY SHARE")) {
            ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
                ProjectOwner.EMBEDDED_CATALOG, "(SELECT %1$s FROM project " + lockingClause + ") IS NOT NULL");

            assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must alias the");
        }
    }

    @Test
    void testContributorsRejectADerivedTableAliasThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM (SELECT 1 AS id) project WHERE project.id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsAcceptAColumnNamedLikeTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM catalog WHERE catalog.project = %1$s)");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("EXISTS (SELECT 1 FROM catalog WHERE catalog.project = project.id)");
    }

    @Test
    void testContributorsAcceptTheIdColumnTableNameInsideAStringLiteral() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND name = 'FROM project'");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("project.id = 1 AND name = 'FROM project'");
    }

    @Test
    void testContributorsRejectALowercaseMultiLineSubqueryThatShadowsTheIdColumnTable() {
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED,
            """
                exists (select 1 from project_deployment
                where project_deployment.id = %1$s)""");

        assertThatThrownBy(
            () -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("project_deployment.id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsRejectAnUppercaseTableOrMixedCaseAliasThatShadowsTheIdColumnTable() {
        ProjectOwnerContributor uppercaseTableProjectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM PROJECT WHERE PROJECT.id = %1$s)");
        ProjectOwnerContributor mixedCaseAliasProjectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM catalog Project WHERE Project.id = %1$s)");

        assertThatThrownBy(() -> uppercaseTableProjectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
        assertThatThrownBy(() -> mixedCaseAliasProjectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must alias the");
    }

    @Test
    void testContributorsAcceptASchemaQualifiedSubqueryThatAliasesTheIdColumnTable() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM public.project owned_project WHERE owned_project.id = %1$s)");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("EXISTS (SELECT 1 FROM public.project owned_project WHERE owned_project.id = project.id)");
    }

    @Test
    void testContributorsAcceptASubqueryThatAliasesTheIdColumnTable() {
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED,
            "EXISTS (SELECT 1 FROM project_deployment owned_deployment WHERE owned_deployment.id = %1$s)");
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM project AS owned_project WHERE owned_project.id = %1$s)");

        assertThat(projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("project_deployment.id"))
            .isEqualTo(
                "EXISTS (SELECT 1 FROM project_deployment owned_deployment WHERE owned_deployment.id = " +
                    "project_deployment.id)");
        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("EXISTS (SELECT 1 FROM project AS owned_project WHERE owned_project.id = project.id)");
    }

    @Test
    void testFactoriesRejectATemplateWithAStrayPercentSign() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND name LIKE 'a%'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format specifiers");
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(
                ProjectDeploymentOwner.EMBEDDED, "%1$s = 1 AND name LIKE '%a'"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("format specifiers");
    }

    @Test
    void testFactoriesRejectAFormatSpecifierOtherThanTheIdColumn() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND name LIKE '%name'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format specifiers");
        assertThatThrownBy(() -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%s = 1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("format specifiers");
    }

    @Test
    void testFactoriesAcceptAnEscapedPercentSign() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND name LIKE 'a%%'");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("project.id = 1 AND name LIKE 'a%'");
    }

    @Test
    void testFactoriesRejectAComment() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 -- note"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("balanced parentheses and string literals");
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s = 1 /* note */"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanced parentheses and string literals");
    }

    @Test
    void testFactoriesRejectAStatementSeparator() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1; DELETE FROM project"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanced parentheses and string literals");
    }

    @Test
    void testFactoriesRejectAQuotedIdentifier() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(
                ProjectOwner.EMBEDDED_CATALOG, "EXISTS (SELECT 1 FROM \"project\" WHERE project.id = %1$s)"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dollar quoted strings");
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(
                ProjectDeploymentOwner.EMBEDDED, "%1$s IS NOT NULL AND \"it's\" IS NULL; DELETE FROM project"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dollar quoted strings");
    }

    @Test
    void testFactoriesRejectABackslashEscape() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s::text = E'\\'' OR 1 = 1 --'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dollar quoted strings");
    }

    @Test
    void testFactoriesRejectADollarQuotedString() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(
                ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND $$'$$ IS NOT NULL) OR (1 = 1 AND $$'$$ IS NOT NULL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dollar quoted strings");
    }

    @Test
    void testFactoriesRejectUnbalancedParentheses() {
        assertThatThrownBy(
            () -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "a = %1$s) OR (1 = 1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanced parentheses and string literals");
        assertThatThrownBy(
            () -> ProjectDeploymentOwnerContributor.of(
                ProjectDeploymentOwner.EMBEDDED, "EXISTS (SELECT 1 FROM a WHERE a.id = %1$s"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("balanced parentheses and string literals");
    }

    @Test
    void testFactoriesRejectAnUnterminatedStringLiteral() {
        assertThatThrownBy(() -> ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, "%1$s::text = 'a"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("balanced parentheses and string literals");
    }

    @Test
    void testFactoriesAcceptSeparatorsAndParenthesesInsideAStringLiteral() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1 AND name = 'it''s (a; -- b /* c'");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isEqualTo("project.id = 1 AND name = 'it''s (a; -- b /* c'");
    }

    @Test
    void testContributorsRejectAnInvalidIdColumn() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1");
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED, "%1$s = 1");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id OR 1 = 1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a table qualified column name");
        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("a.b.c"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a table qualified column name");
        assertThatThrownBy(() -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("1id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a table qualified column name");
        assertThatThrownBy(() -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a table qualified column name");
    }

    @Test
    void testContributorsRejectAnUnqualifiedIdColumn() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG,
            "EXISTS (SELECT 1 FROM automation_workflow_project WHERE automation_workflow_project.project_id = %1$s)");
        ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor = ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.MCP_SERVER,
            "EXISTS (SELECT 1 FROM mcp_project WHERE mcp_project.project_deployment_id = %1$s)");

        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a table qualified column name");
        assertThatThrownBy(() -> projectDeploymentOwnerContributor.getProjectDeploymentClaimCondition("id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a table qualified column name");
    }

    @Test
    void testContributorsAcceptAQualifiedIdColumn() {
        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, "%1$s = 1");

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id")).isEqualTo("project.id = 1");
        assertThat(projectOwnerContributor.getProjectClaimCondition("project_deployment.project_id"))
            .isEqualTo("project_deployment.project_id = 1");
    }

    @Test
    void testContributorsValidateEverySuppliedTemplate() {
        List<String> conditionTemplates = new ArrayList<>(List.of("%1$s = 1", "%1$s = 1; DELETE FROM project"));

        ProjectOwnerContributor projectOwnerContributor = ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_CATALOG, () -> conditionTemplates.remove(0));

        assertThat(projectOwnerContributor.getProjectClaimCondition("project.id")).isEqualTo("project.id = 1");
        assertThatThrownBy(() -> projectOwnerContributor.getProjectClaimCondition("project.id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("balanced parentheses and string literals");
    }
}
