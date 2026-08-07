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

package com.bytechef.platform.ai.auto.memory.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.parser.Parser;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the schema-to-entity resolution of the {@code AiAutoMemory} GraphQL type. Ownership is exposed as the
 * {@code principalType}/{@code principalId} pair the entity actually stores, rather than a {@code userId} the entity
 * never had — that field was a rename in the resolver, and it silently asserted every memory belongs to a user. Each
 * field is resolved by an explicit {@link SchemaMapping}, matching {@code memoryType}'s existing treatment.
 *
 * @author Ivica Cardic
 */
class AiAutoMemoryGraphQlControllerTest {

    private static final long CURRENT_USER_ID = 7;

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final UserService userService = mock(UserService.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    private final AiAutoMemoryGraphQlController aiAutoMemoryGraphQlController = new AiAutoMemoryGraphQlController(
        aiAutoMemoryService, userService, workspaceFacade, projectDeploymentService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPrincipalIdResolvesFromTheEntity() {
        AiAutoMemory memory = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, 42);

        assertThat(aiAutoMemoryGraphQlController.principalId(memory)).isEqualTo(42);
    }

    /**
     * Serialised as the enum name so the GraphQL value matches {@code AiAutoMemoryPrincipalType}. The ordinal is a
     * persistence detail and must never reach the wire — it is append-only precisely because it is positional.
     */
    @Test
    void testPrincipalTypeResolvesToTheEnumName() {
        AiAutoMemory memory = new AiAutoMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 42);

        assertThat(aiAutoMemoryGraphQlController.principalType(memory)).isEqualTo("PROJECT_DEPLOYMENT");
    }

    @Test
    void testEverySchemaFieldIsResolvable() {
        Set<String> schemaMappedFields = getSchemaMappedFields();

        List<String> unresolvable = getSchemaFieldNames()
            .stream()
            .filter(fieldName -> !schemaMappedFields.contains(fieldName))
            .filter(fieldName -> !hasGetter(fieldName))
            .toList();

        assertThat(unresolvable)
            .as("schema fields with neither a @SchemaMapping method nor an AiAutoMemory getter resolve to null")
            .isEmpty();
    }

    private static Set<String> getSchemaMappedFields() {
        return Arrays.stream(AiAutoMemoryGraphQlController.class.getDeclaredMethods())
            .map(method -> method.getAnnotation(SchemaMapping.class))
            .filter(Objects::nonNull)
            .filter(schemaMapping -> "AiAutoMemory".equals(schemaMapping.typeName()))
            .map(SchemaMapping::field)
            .collect(Collectors.toSet());
    }

    private static List<String> getSchemaFieldNames() {
        Document document = new Parser().parseDocument(readSchema());

        ObjectTypeDefinition objectTypeDefinition = document.getDefinitionsOfType(ObjectTypeDefinition.class)
            .stream()
            .filter(definition -> "AiAutoMemory".equals(definition.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("AiAutoMemory type definition not found in schema"));

        return objectTypeDefinition.getFieldDefinitions()
            .stream()
            .map(FieldDefinition::getName)
            .toList();
    }

    private static String readSchema() {
        try (InputStream inputStream = AiAutoMemoryGraphQlControllerTest.class.getResourceAsStream(
            "/graphql/ai-auto-memory.graphqls")) {

            return new String(Objects.requireNonNull(inputStream, "ai-auto-memory.graphqls")
                .readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read ai-auto-memory.graphqls", exception);
        }
    }

    private static boolean hasGetter(String fieldName) {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        for (Method method : AiAutoMemory.class.getMethods()) {
            String methodName = method.getName();

            if ((methodName.equals("get" + suffix) || methodName.equals("is" + suffix))
                && method.getParameterCount() == 0) {

                return true;
            }
        }

        return false;
    }

    @Test
    void testOmittingBothArgumentsDefaultsToTheCurrentUser() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(null, null, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalType()).isEqualTo(AiAutoMemoryPrincipalType.USER);
        assertThat(principal.principalId()).isEqualTo(CURRENT_USER_ID);
    }

    @Test
    void testOwnUserIdIsAddressable() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalId()).isEqualTo(CURRENT_USER_ID);
    }

    /**
     * Two members of one workspace both have rows under that workspaceId, so the lookup key does not isolate them.
     * Denial is a null return rather than an exception so a caller cannot tell "not yours" from "no such memory".
     */
    @Test
    void testAnotherUsersIdIsDenied() {
        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.USER, 99L, CURRENT_USER_ID, false)).isNull();
    }

    @Test
    void testProjectDeploymentIsReadableByAnyMember() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalType()).isEqualTo(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT);
        assertThat(principal.principalId()).isEqualTo(5);
    }

    @Test
    void testProjectDeploymentMutationIsDeniedWithoutAdmin() {
        authenticateWith("ROLE_USER");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, CURRENT_USER_ID, true)).isNull();
    }

    @Test
    void testProjectDeploymentMutationIsAllowedForAdmin() {
        authenticateWith("ROLE_ADMIN");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, CURRENT_USER_ID, true)).isNotNull();
    }

    @Test
    void testIntegrationInstanceIsRejected() {
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L, CURRENT_USER_ID, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INTEGRATION_INSTANCE");
    }

    /**
     * Pins default-deny for whatever {@link #resolvePrincipal} does not explicitly recognize. USER and
     * PROJECT_DEPLOYMENT are pinned by the tests above; every other value — today only INTEGRATION_INSTANCE, but also
     * whatever a future enum addition brings — must never resolve to an addressable principal, so a fourth principal
     * type fails this test instead of silently becoming readable.
     */
    @Test
    void testUnhandledPrincipalTypesAreDeniedByDefault() {
        for (AiAutoMemoryPrincipalType principalType : AiAutoMemoryPrincipalType.values()) {
            if (principalType == AiAutoMemoryPrincipalType.USER
                || principalType == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT) {

                continue;
            }

            assertPrincipalTypeIsUnaddressable(principalType);
        }
    }

    private void assertPrincipalTypeIsUnaddressable(AiAutoMemoryPrincipalType principalType) {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal;

        try {
            principal = aiAutoMemoryGraphQlController.resolvePrincipal(principalType, 5L, CURRENT_USER_ID, false);
        } catch (IllegalArgumentException illegalArgumentException) {
            assertThat(illegalArgumentException.getMessage()).isNotBlank();

            return;
        }

        assertThat(principal)
            .as("%s must not resolve to an addressable principal", principalType)
            .isNull();
    }

    @Test
    void testHalfSpecifiedPrincipalIsRejected() {
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(
                AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, null, CURRENT_USER_ID, false))
                    .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(null, 5L, CURRENT_USER_ID, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testListWithAProjectDeploymentPrincipalQueriesThatPrincipal() {
        givenCurrentUserInWorkspace();

        aiAutoMemoryGraphQlController.aiAutoMemories(
            1L, 0, null, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);

        verify(aiAutoMemoryService).list(
            eq(1L), eq(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT), eq(5L), eq(0), isNull());
    }

    /**
     * A denied principal must not reach the service at all — returning empty is what keeps ids unenumerable.
     */
    @Test
    void testListWithAnotherUsersIdReturnsEmptyWithoutQuerying() {
        givenCurrentUserInWorkspace();

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, AiAutoMemoryPrincipalType.USER, 99L))
            .isEmpty();

        verify(aiAutoMemoryService, never()).list(anyLong(), any(), anyLong(), anyInt(), any());
    }

    @Test
    void testSingleFetchWithAnotherUsersIdReturnsNull() {
        givenCurrentUserInWorkspace();

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemory(1L, 3L, 0, AiAutoMemoryPrincipalType.USER, 99L))
            .isNull();
    }

    /**
     * The environment the session is looking at reaches the service, so a row belonging to another environment is
     * resolved as missing rather than returned.
     */
    @Test
    void testSingleFetchPassesTheEnvironmentToTheService() {
        givenCurrentUserInWorkspace();

        aiAutoMemoryGraphQlController.aiAutoMemory(1L, 3L, 2, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);

        verify(aiAutoMemoryService).findById(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 3L, 2);
    }

    @Test
    void testDeletingAProjectDeploymentMemoryAsAdminReachesTheService() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        aiAutoMemoryGraphQlController.deleteAiAutoMemory(1L, 3L, 2, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);

        verify(aiAutoMemoryService).deleteById(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 3L, 2);
    }

    /**
     * A non-admin gets the not-found shape rather than a distinct authorization error, matching the read path — the
     * caller learns nothing about whether the row exists.
     */
    @Test
    void testDeletingAProjectDeploymentMemoryWithoutAdminThrowsNotFound() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_USER");

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(
                1L, 3L, 2, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).deleteById(anyLong(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testUpdatingAProjectDeploymentMemoryAsAdminReachesTheService() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        aiAutoMemoryGraphQlController.updateAiAutoMemory(
            new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                3L, 1L, 2, "New title", null, null, null, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));

        verify(aiAutoMemoryService).updateById(
            eq(1L), eq(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT), eq(5L), eq(3L), eq("New title"), isNull(),
            isNull(),
            isNull(), eq(2));
    }

    /**
     * The picker must apply the same rules as the read path: offering an owner the reads then refuse would be a bug
     * surface, and revealing that other users hold memory would leak exactly what the foreign-id guard protects.
     */
    @Test
    void testPrincipalsExcludesOtherUsersAndIntegrationInstances() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, 0)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99, 5),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 7, 3)));

        List<AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal> principals =
            aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0);

        assertThat(principals)
            .extracting(
                AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::principalType,
                AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::principalId)
            .containsExactly(
                tuple(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID),
                tuple(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));
    }

    @Test
    void testOwnUserPrincipalIsLabelledWithoutTheWordUser() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, 0)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2)));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .singleElement()
            .extracting(AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::label)
            .isEqualTo("My memories");
    }

    /**
     * The All scope. Absent principal arguments mean every owner the caller may address on THIS query — the opposite
     * default to the by-id query, where absent means the caller.
     */
    @Test
    void testListWithoutAPrincipalReadsEveryOwnerItMayAddress() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, 0)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2)));

        AiAutoMemory own = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID);

        when(aiAutoMemoryService.listAllOwners(1L, 0, null)).thenReturn(List.of(own));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null, null))
            .containsExactly(own);

        // The per-principal read must not run at all — All is one owner-agnostic query, not a fan-out.
        verify(aiAutoMemoryService, never()).list(anyLong(), any(), anyLong(), anyInt(), any());
    }

    /**
     * The owner-agnostic read carries no authorization of its own, so the controller drops what the picker would not
     * have offered. Without this filter the All scope would leak deployment-owned memories to a non-admin.
     */
    @Test
    void testListWithoutAPrincipalDropsOwnersTheCallerCannotAddress() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, 0)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99L, 1)));

        AiAutoMemory own = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID);
        AiAutoMemory someoneElses = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, 99L);

        when(aiAutoMemoryService.listAllOwners(1L, 0, null)).thenReturn(List.of(own, someoneElses));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null, null))
            .containsExactly(own);
    }

    private void givenCurrentUserInWorkspace() {
        User user = new User();

        user.setId(CURRENT_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace = new Workspace();

        workspace.setId(1L);

        when(workspaceFacade.getUserWorkspaces(CURRENT_USER_ID)).thenReturn(List.of(workspace));
    }

    private static void authenticateWith(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "tester", "password", List.of(new SimpleGrantedAuthority(authority))));
    }
}
