/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider.ScopeDefinition;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiPlatformPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DataTablePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ExecutionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.KnowledgeBasePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.AppEventPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.ConnectedUserPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedSharedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationInstanceConfigurationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationPermissionScopeProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionScopeRegistryTest {

    private static final Set<String> EXPECTED_VIEWER_SCOPES = Set.of("ALPHA_VIEW", "BETA_VIEW");

    private static final Set<String> EDITOR_DELTA_SCOPES = Set.of("ALPHA_EDIT", "BETA_DELETE");

    private static final Set<String> ADMIN_DELTA_SCOPES = Set.of("ALPHA_MANAGE");

    // Synthetic providers exercise the registry's cross-provider aggregation and role tiering without coupling the test
    // to the production scope catalog (the real security.scope.* providers are contributed and verified separately).
    // Scopes are spread across all three tiers so the VIEWER ⊆ EDITOR ⊆ ADMIN invariant is actually exercised.
    private final PermissionScopeRegistry permissionScopeRegistry = new PermissionScopeRegistry(
        List.<PermissionScopeProvider>of(
            () -> Set.of(
                new ScopeDefinition(TestPermissionScope.ALPHA_VIEW, WorkspaceRole.VIEWER),
                new ScopeDefinition(TestPermissionScope.ALPHA_EDIT, WorkspaceRole.EDITOR),
                new ScopeDefinition(TestPermissionScope.ALPHA_MANAGE, WorkspaceRole.ADMIN)),
            () -> Set.of(
                new ScopeDefinition(TestPermissionScope.BETA_VIEW, WorkspaceRole.VIEWER),
                new ScopeDefinition(TestPermissionScope.BETA_DELETE, WorkspaceRole.EDITOR))));

    @Test
    void testBuiltInRoleTiers() {
        Set<String> expectedViewer = EXPECTED_VIEWER_SCOPES;
        Set<String> expectedEditor = union(expectedViewer, EDITOR_DELTA_SCOPES);
        Set<String> expectedAdmin = union(expectedEditor, ADMIN_DELTA_SCOPES);

        assertThat(permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER))
            .containsExactlyInAnyOrderElementsOf(expectedViewer);
        assertThat(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR))
            .containsExactlyInAnyOrderElementsOf(expectedEditor);
        assertThat(permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN))
            .containsExactlyInAnyOrderElementsOf(expectedAdmin);
    }

    @Test
    void testAllScopeNames() {
        Set<String> expectedAll = union(union(EXPECTED_VIEWER_SCOPES, EDITOR_DELTA_SCOPES), ADMIN_DELTA_SCOPES);

        assertThat(permissionScopeRegistry.getAllScopeNames()).containsExactlyInAnyOrderElementsOf(expectedAll);
        assertThat(expectedAll).hasSize(5);
    }

    @Test
    void testViewerSubsetEditorSubsetAdmin() {
        Set<String> viewer = permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER);
        Set<String> editor = permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR);
        Set<String> admin = permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN);

        assertThat(editor).containsAll(viewer);
        assertThat(admin).containsAll(editor);
    }

    @Test
    void testBuiltInRolesReadLeastPrivilegedFirstAndCarryInheritedScopes() {
        // Read downwards, each tier shows what the one above adds — which only works if the scopes a role inherits are
        // included rather than only its delta. WorkspaceRole declares itself most-privileged-first, so this is not the
        // enum's own order and cannot be left to it.
        assertThat(permissionScopeRegistry.getBuiltInRoles())
            .extracting(BuiltInRoleDTO::name)
            .containsExactly("VIEWER", "EDITOR", "ADMIN");

        for (BuiltInRoleDTO builtInRole : permissionScopeRegistry.getBuiltInRoles()) {
            assertThat(builtInRole.scopes())
                .containsExactlyInAnyOrderElementsOf(
                    permissionScopeRegistry.getScopeNames(WorkspaceRole.valueOf(builtInRole.name())));
        }
    }

    @Test
    void testScopeGroupsComeFromTheDeclaringEnum() {
        // A module owns an enum, not a provider — two providers contributing constants of the same enum are one
        // module, and a module's own second enum would be its own group. Grouping on the provider instead would let
        // an EE/CE split of the same module render as two headings for one thing.
        PermissionScopeRegistry registry = new PermissionScopeRegistry(
            List.<PermissionScopeProvider>of(
                () -> Set.of(new ScopeDefinition(TestPermissionScope.ALPHA_EDIT, WorkspaceRole.EDITOR)),
                () -> Set.of(new ScopeDefinition(TestPermissionScope.ALPHA_VIEW, WorkspaceRole.VIEWER)),
                () -> Set.of(new ScopeDefinition(OtherModulePermissionScope.OTHER_VIEW, WorkspaceRole.VIEWER))));

        // Groups sorted by module and scopes in the enum's declaration order: the catalogue must read identically on
        // every boot, and the providers hand their scopes over in Set order, which is neither.
        assertThat(registry.getScopeGroups())
            .containsExactly(
                new PermissionScopeGroupDTO("OTHER_MODULE", List.of("OTHER_VIEW")),
                new PermissionScopeGroupDTO("TEST", List.of("ALPHA_VIEW", "ALPHA_EDIT")));
    }

    @Test
    void testScopeGroupsCoverEveryRegisteredScopeExactlyOnce() {
        List<String> groupedScopeNames = permissionScopeRegistry.getScopeGroups()
            .stream()
            .flatMap(scopeGroup -> scopeGroup.scopes()
                .stream())
            .toList();

        // A scope missing from the grouped view is a permission no operator can grant through the editor, while a
        // duplicated one is a checkbox that appears twice under different headings.
        assertThat(groupedScopeNames).doesNotHaveDuplicates();
        assertThat(groupedScopeNames)
            .containsExactlyInAnyOrderElementsOf(permissionScopeRegistry.getAllScopeNames());
    }

    @Test
    void testConflictingMinimumRoleFailsFast() {
        PermissionScopeProvider first =
            () -> Set.of(new ScopeDefinition(TestPermissionScope.X_SCOPE, WorkspaceRole.VIEWER));
        PermissionScopeProvider second =
            () -> Set.of(new ScopeDefinition(TestPermissionScope.X_SCOPE, WorkspaceRole.ADMIN));
        List<PermissionScopeProvider> permissionScopeProviders = List.of(first, second);

        assertThatThrownBy(() -> new PermissionScopeRegistry(permissionScopeProviders))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testAScopeDeclaredOnBothSidesWithTheSameMinimumRoleBelongsToBoth() {
        PermissionScopeRegistry registry = new PermissionScopeRegistry(
            List.of(
                automationProvider(new ScopeDefinition(TestPermissionScope.WORKFLOW_VIEW, WorkspaceRole.VIEWER)),
                embeddedProvider(new ScopeDefinition(TestPermissionScope.WORKFLOW_VIEW, WorkspaceRole.VIEWER))));

        assertThat(registry.getAllScopeNames(PermissionScopeSide.AUTOMATION)).contains("WORKFLOW_VIEW");
        assertThat(registry.getAllScopeNames(PermissionScopeSide.EMBEDDED)).contains("WORKFLOW_VIEW");
    }

    @Test
    void testAnEmbeddedOnlyScopeIsAbsentFromTheAutomationSide() {
        PermissionScopeRegistry registry = new PermissionScopeRegistry(
            List.of(
                embeddedProvider(new ScopeDefinition(TestPermissionScope.INTEGRATION_CREATE, WorkspaceRole.EDITOR))));

        assertThat(registry.getAllScopeNames()).doesNotContain("INTEGRATION_CREATE");
        assertThat(registry.getScopeNames(WorkspaceRole.EDITOR)).doesNotContain("INTEGRATION_CREATE");
        assertThat(registry.getScopeNames(WorkspaceRole.VIEWER, PermissionScopeSide.EMBEDDED))
            .doesNotContain("INTEGRATION_CREATE");
        assertThat(registry.getScopeNames(WorkspaceRole.EDITOR, PermissionScopeSide.EMBEDDED))
            .contains("INTEGRATION_CREATE");
    }

    @Test
    void testConflictingMinimumRolesAcrossSidesStillFailStartup() {
        List<PermissionScopeProvider> permissionScopeProviders = List.of(
            automationProvider(new ScopeDefinition(TestPermissionScope.MCP_DELETE, WorkspaceRole.ADMIN)),
            embeddedProvider(new ScopeDefinition(TestPermissionScope.MCP_DELETE, WorkspaceRole.EDITOR)));

        assertThatThrownBy(() -> new PermissionScopeRegistry(permissionScopeProviders))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("conflicting minimum roles");
    }

    @Test
    void testTheEmbeddedCatalogueHoldsEveryEmbeddedScopeWithItsMinimumRole() {
        PermissionScopeRegistry registry = new PermissionScopeRegistry(
            List.<PermissionScopeProvider>of(
                new IntegrationPermissionScopeProvider(), new IntegrationInstanceConfigurationPermissionScopeProvider(),
                new ConnectedUserPermissionScopeProvider(), new AppEventPermissionScopeProvider(),
                new EmbeddedPermissionScopeProvider(), new EmbeddedSharedPermissionScopeProvider(),
                new WorkflowPermissionScopeProvider(), new ExecutionPermissionScopeProvider(),
                new ConnectionPermissionScopeProvider(), new McpPermissionScopeProvider(),
                new ApiKeyPermissionScopeProvider()));

        assertThat(registry.getScopeNames(WorkspaceRole.VIEWER, PermissionScopeSide.EMBEDDED))
            .containsExactlyInAnyOrder(
                "WORKFLOW_VIEW", "EXECUTION_VIEW", "CONNECTION_VIEW", "MCP_VIEW",
                "INTEGRATION_INSTANCE_CONFIGURATION_VIEW", "CONNECTED_USER_VIEW", "APP_EVENT_VIEW", "EMBEDDED_VIEW");
        assertThat(registry.getScopeNames(WorkspaceRole.ADMIN, PermissionScopeSide.EMBEDDED))
            .contains(
                "INTEGRATION_SETTINGS", "INTEGRATION_DELETE", "EMBEDDED_MEMBER_MANAGE", "EMBEDDED_SETTINGS_MANAGE");

        Set<String> editorScopeNames = registry.getScopeNames(WorkspaceRole.EDITOR, PermissionScopeSide.EMBEDDED);

        assertThat(editorScopeNames)
            .contains("API_KEY_VIEW", "API_KEY_CREATE", "API_KEY_DELETE", "EXECUTION_DETAILS_VIEW")
            .doesNotContain("INTEGRATION_DELETE");
        assertThat(registry.getAllScopeNames())
            .doesNotContain("INTEGRATION_CREATE", "CONNECTED_USER_VIEW", "APP_EVENT_VIEW", "EMBEDDED_VIEW");
    }

    private static PermissionScopeProvider automationProvider(ScopeDefinition... scopeDefinitions) {
        return () -> Set.of(scopeDefinitions);
    }

    private static PermissionScopeProvider embeddedProvider(ScopeDefinition... scopeDefinitions) {
        return new PermissionScopeProvider() {

            @Override
            public Set<ScopeDefinition> scopeDefinitions() {
                return Set.of(scopeDefinitions);
            }

            @Override
            public PermissionScopeSide side() {
                return PermissionScopeSide.EMBEDDED;
            }
        };
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        return Stream.concat(first.stream(), second.stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    // Synthetic scope enum kept local to the test so aggregation/tiering is exercised without coupling to the
    // production scope catalog (the real security.scope.* enums are contributed and verified separately).
    private enum TestPermissionScope implements PermissionScopeType {

        ALPHA_VIEW,
        ALPHA_EDIT,
        ALPHA_MANAGE,
        BETA_VIEW,
        BETA_DELETE,
        X_SCOPE,
        WORKFLOW_VIEW,
        INTEGRATION_CREATE,
        MCP_DELETE
    }

    @Nested
    class ShippedCatalog {

        private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/");

        private static final Pattern GATE_ANNOTATION_PATTERN = Pattern.compile(
            "@(?:PreAuthorize|PostAuthorize|PostFilter)\\(");

        private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("(?m)^\\s*//.*$");

        // A scope is upper snake case, which is what separates it from the resource token beside it ('Project',
        // 'Connection'), so every such literal inside a gate is a scope whichever function names it.
        private static final Pattern GATE_SCOPE_LITERAL_PATTERN = Pattern.compile("'([A-Z][A-Z0-9_]{2,})'");

        private static final Pattern PROGRAMMATIC_SCOPE_CHECK_PATTERN = Pattern.compile(
            "\\.(?:hasWorkspaceScope|hasWorkspaceScopeForProject|hasResourceScope)\\(([^)]*)\\)");

        private static final Pattern SCOPE_NAME_LITERAL_PATTERN = Pattern.compile("\"([A-Z][A-Z0-9_]{2,})\"");

        private static final Pattern EMBEDDED_SCOPE_CALL_PATTERN = Pattern.compile(
            "hasEmbeddedScope(?:InEnvironment|InEnvironmentId|InEveryEnvironment)?\\(([^)]*)\\)");

        private final PermissionScopeRegistry shippedPermissionScopeRegistry = new PermissionScopeRegistry(
            List.<PermissionScopeProvider>of(
                new ApiKeyPermissionScopeProvider(), new ApiPlatformPermissionScopeProvider(),
                new ConnectionPermissionScopeProvider(), new DataTablePermissionScopeProvider(),
                new DeploymentPermissionScopeProvider(), new ExecutionPermissionScopeProvider(),
                new KnowledgeBasePermissionScopeProvider(), new McpPermissionScopeProvider(),
                new ProjectPermissionScopeProvider(), new WorkflowPermissionScopeProvider(),
                new WorkspacePermissionScopeProvider()));

        private final PermissionScopeRegistry shippedEmbeddedPermissionScopeRegistry = new PermissionScopeRegistry(
            List.<PermissionScopeProvider>of(
                new AppEventPermissionScopeProvider(), new ConnectedUserPermissionScopeProvider(),
                new EmbeddedPermissionScopeProvider(), new EmbeddedSharedPermissionScopeProvider(),
                new IntegrationInstanceConfigurationPermissionScopeProvider(),
                new IntegrationPermissionScopeProvider()));

        @Test
        void testEveryScopeNamedByAGateIsRegisteredOnItsOwnSide() {
            Map<PermissionScopeSide, Set<String>> gatedScopeNamesBySide = readScopeNamesNamedByGatesBySide();
            Set<String> gatedScopeNames = readScopeNamesNamedByGates();

            assertThat(gatedScopeNames)
                .as("no @PreAuthorize gate may name a scope that no PermissionScopeProvider declares")
                .isNotEmpty();
            assertThat(shippedEmbeddedPermissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED))
                .as("the hasEmbeddedScope built-ins may only name embedded-side scopes")
                .containsAll(gatedScopeNamesBySide.get(PermissionScopeSide.EMBEDDED));
            assertThat(shippedPermissionScopeRegistry.getAllScopeNames(PermissionScopeSide.AUTOMATION))
                .as("every other gate may only name automation-side scopes")
                .containsAll(gatedScopeNamesBySide.get(PermissionScopeSide.AUTOMATION));
        }

        @Test
        void testEveryRegisteredScopeIsNamedByAGate() {
            Set<String> registeredScopeNames = shippedPermissionScopeRegistry.getAllScopeNames();
            Set<String> gatedScopeNames = readScopeNamesNamedByGates();

            assertThat(gatedScopeNames)
                .as("no PermissionScopeProvider may declare a scope that no @PreAuthorize gate or permission check enforces")
                .containsAll(registeredScopeNames);
        }

        @Test
        void testMcpScopesAreGrantedFromViewerAndEditor() {
            assertThat(shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER)).contains("MCP_VIEW");
            assertThat(shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR)).contains(
                "MCP_VIEW", "MCP_CREATE", "MCP_EDIT");
            assertThat(shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER)).doesNotContain(
                "MCP_CREATE", "MCP_EDIT");
        }

        @Test
        void testConnectionScopesAreGrantedFromViewerAndEditor() {
            assertThat(shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER)).contains("CONNECTION_VIEW");
            assertThat(shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER)).doesNotContain(
                "CONNECTION_CREATE", "CONNECTION_EDIT", "CONNECTION_DELETE");
            assertThat(shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR)).contains(
                "CONNECTION_VIEW", "CONNECTION_CREATE", "CONNECTION_EDIT", "CONNECTION_DELETE");
        }

        @Test
        void testViewerSubsetEditorSubsetAdmin() {
            Set<String> viewerScopeNames = shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER);
            Set<String> editorScopeNames = shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR);
            Set<String> adminScopeNames = shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN);

            assertThat(editorScopeNames).containsAll(viewerScopeNames);
            assertThat(adminScopeNames).containsAll(editorScopeNames);
        }

        private static Set<String> readScopeNamesNamedByGates() {
            Set<String> scopeNames = new TreeSet<>();

            for (Set<String> scopeNamesOfSide : readScopeNamesNamedByGatesBySide().values()) {
                scopeNames.addAll(scopeNamesOfSide);
            }

            return scopeNames;
        }

        private static Map<PermissionScopeSide, Set<String>> readScopeNamesNamedByGatesBySide() {
            Path serverPath = findRepositoryRootPath().resolve("server");
            Set<String> automationScopeNames = new TreeSet<>();
            Set<String> embeddedScopeNames = new TreeSet<>();

            try (Stream<Path> paths = Files.walk(serverPath)) {
                List<Path> javaPaths = paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String pathString = path.toString();

                        return pathString.endsWith(".java") && pathString.contains("/src/main/") &&
                            !pathString.contains("/build/");
                    })
                    .toList();

                for (Path javaPath : javaPaths) {
                    String source = stripComments(Files.readString(javaPath));

                    Matcher gateAnnotationMatcher = GATE_ANNOTATION_PATTERN.matcher(source);

                    while (gateAnnotationMatcher.find()) {
                        String annotationArguments = readAnnotationArguments(source, gateAnnotationMatcher.end());

                        Matcher embeddedCallMatcher = EMBEDDED_SCOPE_CALL_PATTERN.matcher(annotationArguments);

                        while (embeddedCallMatcher.find()) {
                            collectScopeLiterals(embeddedCallMatcher.group(1), embeddedScopeNames);
                        }

                        collectScopeLiterals(
                            EMBEDDED_SCOPE_CALL_PATTERN.matcher(annotationArguments)
                                .replaceAll(""),
                            automationScopeNames);
                    }

                    Matcher programmaticScopeCheckMatcher = PROGRAMMATIC_SCOPE_CHECK_PATTERN.matcher(source);

                    while (programmaticScopeCheckMatcher.find()) {
                        Matcher scopeNameLiteralMatcher = SCOPE_NAME_LITERAL_PATTERN.matcher(
                            programmaticScopeCheckMatcher.group(1));

                        while (scopeNameLiteralMatcher.find()) {
                            automationScopeNames.add(scopeNameLiteralMatcher.group(1));
                        }
                    }
                }
            } catch (IOException ioException) {
                throw new UncheckedIOException(ioException);
            }

            return Map.of(
                PermissionScopeSide.AUTOMATION, automationScopeNames, PermissionScopeSide.EMBEDDED,
                embeddedScopeNames);
        }

        private static void collectScopeLiterals(String expression, Set<String> scopeNames) {
            Matcher gateScopeLiteralMatcher = GATE_SCOPE_LITERAL_PATTERN.matcher(expression);

            while (gateScopeLiteralMatcher.find()) {
                String literal = gateScopeLiteralMatcher.group(1);

                if (!literal.startsWith("ROLE_")) {
                    scopeNames.add(literal);
                }
            }
        }

        private static String readAnnotationArguments(String source, int startIndex) {
            int depth = 1;
            int index = startIndex;

            while (index < source.length() && depth > 0) {
                char character = source.charAt(index);

                if (character == '(') {
                    depth++;
                } else if (character == ')') {
                    depth--;
                }

                index++;
            }

            return source.substring(startIndex, index);
        }

        private static String stripComments(String source) {
            String withoutBlockComments = BLOCK_COMMENT_PATTERN.matcher(source)
                .replaceAll("");

            return LINE_COMMENT_PATTERN.matcher(withoutBlockComments)
                .replaceAll("");
        }

        private static Path findRepositoryRootPath() {
            Path path = Path.of("")
                .toAbsolutePath();

            while (path != null && !Files.exists(path.resolve("settings.gradle.kts"))) {
                path = path.getParent();
            }

            assertThat(path)
                .as("repository root containing settings.gradle.kts must be locatable from the test working directory")
                .isNotNull();

            return path;
        }
    }

    // A second enum, so the grouping is pinned to the declaring enum rather than to the provider that hands it over.
    private enum OtherModulePermissionScope implements PermissionScopeType {

        OTHER_VIEW
    }
}
