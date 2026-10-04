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
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider.ScopeDefinition;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    void testConflictingMinimumRoleFailsFast() {
        PermissionScopeProvider first =
            () -> Set.of(new ScopeDefinition(TestPermissionScope.X_SCOPE, WorkspaceRole.VIEWER));
        PermissionScopeProvider second =
            () -> Set.of(new ScopeDefinition(TestPermissionScope.X_SCOPE, WorkspaceRole.ADMIN));

        assertThatThrownBy(() -> new PermissionScopeRegistry(List.of(first, second)))
            .isInstanceOf(IllegalStateException.class);
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
        X_SCOPE
    }

    @Nested
    class ShippedCatalog {

        private static final Pattern HAS_PERMISSION_PATTERN = Pattern.compile(
            "hasPermission\\([^)]*,\\s*'[A-Za-z]+'\\s*,\\s*'([A-Z][A-Z0-9_]{2,})'\\s*\\)");

        private static final Pattern PROGRAMMATIC_SCOPE_CHECK_PATTERN = Pattern.compile(
            "\\.(?:hasWorkspaceScope|hasWorkspaceScopeForProject|hasResourceScope)\\(([^)]*)\\)");

        private static final Pattern SCOPE_NAME_LITERAL_PATTERN = Pattern.compile("\"([A-Z][A-Z0-9_]{2,})\"");

        private final PermissionScopeRegistry shippedPermissionScopeRegistry = new PermissionScopeRegistry(
            List.<PermissionScopeProvider>of(
                new DeploymentPermissionScopeProvider(), new McpPermissionScopeProvider(),
                new ProjectPermissionScopeProvider(), new WorkflowPermissionScopeProvider()));

        @Test
        void testEveryScopeNamedByAGateIsRegistered() {
            Set<String> registeredScopeNames = shippedPermissionScopeRegistry.getAllScopeNames();
            Set<String> gatedScopeNames = readScopeNamesNamedByGates();

            assertThat(gatedScopeNames)
                .as("no @PreAuthorize gate may name a scope that no PermissionScopeProvider declares")
                .isNotEmpty();
            assertThat(registeredScopeNames).containsAll(gatedScopeNames);
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
        void testViewerSubsetEditorSubsetAdmin() {
            Set<String> viewerScopeNames = shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER);
            Set<String> editorScopeNames = shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR);
            Set<String> adminScopeNames = shippedPermissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN);

            assertThat(editorScopeNames).containsAll(viewerScopeNames);
            assertThat(adminScopeNames).containsAll(editorScopeNames);
        }

        private static Set<String> readScopeNamesNamedByGates() {
            Path serverPath = findRepositoryRootPath().resolve("server");
            Set<String> scopeNames = new TreeSet<>();

            try (Stream<Path> paths = Files.walk(serverPath)) {
                List<Path> javaPaths = paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String pathString = path.toString();

                        return pathString.endsWith(".java") && pathString.contains("/src/main/") &&
                            !pathString.contains("/build/");
                    })
                    .toList();

                for (Path javaPath : javaPaths) {
                    String source = Files.readString(javaPath);

                    Matcher hasPermissionMatcher = HAS_PERMISSION_PATTERN.matcher(source);

                    while (hasPermissionMatcher.find()) {
                        scopeNames.add(hasPermissionMatcher.group(1));
                    }

                    Matcher programmaticScopeCheckMatcher = PROGRAMMATIC_SCOPE_CHECK_PATTERN.matcher(source);

                    while (programmaticScopeCheckMatcher.find()) {
                        Matcher scopeNameLiteralMatcher = SCOPE_NAME_LITERAL_PATTERN.matcher(
                            programmaticScopeCheckMatcher.group(1));

                        while (scopeNameLiteralMatcher.find()) {
                            scopeNames.add(scopeNameLiteralMatcher.group(1));
                        }
                    }
                }
            } catch (IOException ioException) {
                throw new UncheckedIOException(ioException);
            }

            return scopeNames;
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
}
