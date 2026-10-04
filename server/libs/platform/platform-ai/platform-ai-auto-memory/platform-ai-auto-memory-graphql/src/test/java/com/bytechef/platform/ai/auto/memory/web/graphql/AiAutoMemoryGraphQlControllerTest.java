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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AiAutoMemoryGraphQlControllerTest {

    private static final long CURRENT_USER_ID = 7;

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final UserService userService = mock(UserService.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final AiAutoMemoryGraphQlController aiAutoMemoryGraphQlController = new AiAutoMemoryGraphQlController(
        aiAutoMemoryService, userService, workspaceFacade, projectDeploymentService, projectService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPrincipalIdResolvesFromTheEntity() {
        AiAutoMemory memory = newMemory(AiAutoMemoryPrincipalType.USER, 42);

        assertThat(aiAutoMemoryGraphQlController.principalId(memory)).isEqualTo(42);
    }

    @Test
    void testPrincipalTypeResolvesToTheEnumName() {
        AiAutoMemory memory = newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 42);

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
            aiAutoMemoryGraphQlController.resolvePrincipal(null, CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalType()).isEqualTo(AiAutoMemoryPrincipalType.USER);
        assertThat(principal.principalId()).isEqualTo(CURRENT_USER_ID);
    }

    @Test
    void testOwnUserIdIsAddressable() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID), CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalId()).isEqualTo(CURRENT_USER_ID);
    }

    @Test
    void testAnotherUsersIdIsDenied() {
        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.USER, 99L), CURRENT_USER_ID, false)).isNull();
    }

    @Test
    void testProjectDeploymentIsReadableByAnyMember() {
        AiAutoMemoryGraphQlController.ResolvedPrincipal principal =
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L), CURRENT_USER_ID, false);

        assertThat(principal).isNotNull();
        assertThat(principal.principalType()).isEqualTo(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT);
        assertThat(principal.principalId()).isEqualTo(5);
    }

    @Test
    void testProjectDeploymentMutationIsDeniedWithoutAdmin() {
        authenticateWith("ROLE_USER");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L), CURRENT_USER_ID, true)).isNull();
    }

    @Test
    void testProjectDeploymentMutationIsAllowedForAdmin() {
        authenticateWith("ROLE_ADMIN");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L), CURRENT_USER_ID, true)).isNotNull();
    }

    @Test
    void testIntegrationInstanceIsRejected() {
        AiAutoMemoryGraphQlController.AiAutoMemoryPrincipalInput principalInput = principal(
            AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L);

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.resolvePrincipal(principalInput, CURRENT_USER_ID, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INTEGRATION_INSTANCE");
    }

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
            principal =
                aiAutoMemoryGraphQlController.resolvePrincipal(principal(principalType, 5L), CURRENT_USER_ID, false);
        } catch (IllegalArgumentException illegalArgumentException) {
            assertThat(illegalArgumentException.getMessage()).isNotBlank();

            return;
        }

        assertThat(principal)
            .as("%s must not resolve to an addressable principal", principalType)
            .isNull();
    }

    @Test
    void testListWithAProjectDeploymentPrincipalQueriesThatPrincipal() {
        givenCurrentUserInWorkspace();

        aiAutoMemoryGraphQlController.aiAutoMemories(
            1L, 0, null, principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.DEVELOPMENT), null);
    }

    @Test
    void testListWithAnotherUsersIdReturnsEmptyWithoutQuerying() {
        givenCurrentUserInWorkspace();

        assertThat(
            aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, principal(AiAutoMemoryPrincipalType.USER, 99L)))
                .isEmpty();

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testSingleFetchWithAnotherUsersIdReturnsNull() {
        givenCurrentUserInWorkspace();

        assertThat(
            aiAutoMemoryGraphQlController.aiAutoMemory(1L, 3L, 0, principal(AiAutoMemoryPrincipalType.USER, 99L)))
                .isNull();
    }

    @Test
    void testSingleFetchPassesTheEnvironmentToTheService() {
        givenCurrentUserInWorkspace();

        aiAutoMemoryGraphQlController.aiAutoMemory(1L, 3L, 2,
            principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));

        verify(aiAutoMemoryService).findById(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION), 3L);
    }

    @Test
    void testDeletingAProjectDeploymentMemoryAsAdminReachesTheService() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        aiAutoMemoryGraphQlController.deleteAiAutoMemory(1L, 3L, 2,
            principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L));

        verify(aiAutoMemoryService).deleteById(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION), 3L);
    }

    @Test
    void testDeletingAProjectDeploymentMemoryWithoutAdminThrowsNotFound() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_USER");

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(
                1L, 3L, 2, principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L)))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testUpdatingAProjectDeploymentMemoryAsAdminReachesTheService() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        aiAutoMemoryGraphQlController.updateAiAutoMemory(
            new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                3L, 1L, 2, 4L, "New title", null, null, null,
                principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L)));

        verify(aiAutoMemoryService).updateById(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION), 3L,
            4L, new AiAutoMemoryPatch("New title", null, null, null));
    }

    @Test
    void testUpdatingAProjectDeploymentMemoryWithoutAdminThrowsNotFound() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_USER");

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.updateAiAutoMemory(
                new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                    3L, 1L, 2, 0L, "New title", null, null, null,
                    principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT,
                        5L))))
                            .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
    }

    @Test
    void testEmbeddedConnectedUserDeploymentMemoryIsNotAddressable() {
        givenCurrentUserInWorkspace();
        givenEmbeddedDeployment(5L);
        authenticateWith("ROLE_ADMIN");

        assertThat(
            aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null,
                principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L)))
                    .isEmpty();
        assertThat(
            aiAutoMemoryGraphQlController.aiAutoMemory(1L, 3L, 0,
                principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L)))
                    .isNull();
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.updateAiAutoMemory(
                new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                    3L, 1L, 0, 0L, "New title", null, null, null,
                    principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT,
                        5L))))
                            .isInstanceOf(AiAutoMemoryNotFoundException.class);
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(
                1L, 3L, 0, principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L)))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).list(any(), any());
        verify(aiAutoMemoryService, never()).findById(any(), anyLong());
        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testListingsDropEmbeddedConnectedUserDeployments() {
        givenCurrentUserInWorkspace();
        givenDeployments(deployment(5L, 50L, "Orders"), deployment(6L, 60L, "__EMBEDDED__customer-42"));
        givenProjects(project(50L, "Orders"), project(60L, "__EMBEDDED__customer-42"));

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 1)));

        AiAutoMemory deploymentMemory = newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);
        AiAutoMemory embeddedMemory = newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(deploymentMemory, embeddedMemory));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .extracting(
                AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::principalId,
                AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::label)
            .containsExactly(tuple(5L, "Orders"));
        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null))
            .containsExactly(deploymentMemory);
    }

    @Test
    void testPrincipalsExcludesOtherUsersAndIntegrationInstances() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
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

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2)));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .singleElement()
            .extracting(AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::label)
            .isEqualTo("My memories");
    }

    @Test
    void testListWithoutAPrincipalReadsEveryOwnerItMayAddress() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 2)));

        AiAutoMemory own = newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null)).thenReturn(List.of(own));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null))
            .containsExactly(own);

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testListWithoutAPrincipalPassesTheMemoryTypeThrough() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 1)));

        AiAutoMemory own = newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, AiAutoMemoryType.FEEDBACK))
            .thenReturn(List.of(own));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, AiAutoMemoryType.FEEDBACK, null))
            .containsExactly(own);
    }

    @Test
    void testADeletedDeploymentsMemoryStaysReachableForCleanup() {
        givenCurrentUserInWorkspace();
        givenDeployments();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 1)));

        AiAutoMemory orphanedMemory = newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(orphanedMemory));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .extracting(AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::label)
            .containsExactly("Deployment 6");
        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null))
            .containsExactly(orphanedMemory);

        aiAutoMemoryGraphQlController.aiAutoMemories(
            1L, 0, null, principal(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L));

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, Environment.DEVELOPMENT),
            null);
    }

    @Test
    void testADeploymentWhoseProjectIsGoneIsNotTreatedAsEmbedded() {
        givenCurrentUserInWorkspace();
        givenDeployments(deployment(6L, 60L, "Orders"));
        givenProjects();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 6L, 1)));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .extracting(AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::label)
            .containsExactly("Orders");
    }

    @Test
    void testListWithoutAPrincipalDropsOwnersTheCallerCannotAddress() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99L, 1)));

        AiAutoMemory own = newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID);
        AiAutoMemory someoneElses = newMemory(AiAutoMemoryPrincipalType.USER, 99L);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(own, someoneElses));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null))
            .containsExactly(own);
    }

    @Test
    void testListWithoutAPrincipalDropsIntegrationInstanceMemories() {
        givenCurrentUserInWorkspace();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L, 1)));

        AiAutoMemory deploymentMemory = newMemory(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L);
        AiAutoMemory integrationInstanceMemory = newMemory(AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 5L);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(deploymentMemory, integrationInstanceMemory));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null))
            .containsExactly(deploymentMemory);
    }

    @Test
    void testMutatingAnotherUsersMemoryThrowsNotFoundWithoutReachingTheService() {
        givenCurrentUserInWorkspace();

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.updateAiAutoMemory(
                new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                    3L, 1L, 0, 0L, "New title", null, null, null, principal(AiAutoMemoryPrincipalType.USER, 99L))))
                        .isInstanceOf(AiAutoMemoryNotFoundException.class);
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(1L, 3L, 0,
                principal(AiAutoMemoryPrincipalType.USER, 99L)))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testAnAdminCannotAddressAnotherUsersMemory() {
        givenCurrentUserInWorkspace();
        authenticateWith("ROLE_ADMIN");

        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.USER, 99L), CURRENT_USER_ID, false)).isNull();
        assertThat(
            aiAutoMemoryGraphQlController.resolvePrincipal(
                principal(AiAutoMemoryPrincipalType.USER, 99L), CURRENT_USER_ID, true)).isNull();

        when(aiAutoMemoryService.listPrincipals(1L, Environment.DEVELOPMENT)).thenReturn(
            List.of(
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID, 1),
                new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 99L, 1)));

        AiAutoMemory own = newMemory(AiAutoMemoryPrincipalType.USER, CURRENT_USER_ID);

        when(aiAutoMemoryService.listAllOwners(1L, Environment.DEVELOPMENT, null))
            .thenReturn(List.of(own, newMemory(AiAutoMemoryPrincipalType.USER, 99L)));

        assertThat(aiAutoMemoryGraphQlController.aiAutoMemories(1L, 0, null, null)).containsExactly(own);
        assertThat(aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(1L, 0))
            .extracting(AiAutoMemoryGraphQlController.AiAutoMemoryPrincipal::principalId)
            .containsExactly(CURRENT_USER_ID);

        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.updateAiAutoMemory(
                new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                    3L, 1L, 0, 0L, "New title", null, null, null, principal(AiAutoMemoryPrincipalType.USER, 99L))))
                        .isInstanceOf(AiAutoMemoryNotFoundException.class);
        assertThatThrownBy(
            () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(1L, 3L, 0,
                principal(AiAutoMemoryPrincipalType.USER, 99L)))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiAutoMemoryService, never()).updateById(any(), anyLong(), anyLong(), any());
        verify(aiAutoMemoryService, never()).deleteById(any(), anyLong());
    }

    @Test
    void testEveryEntryPointRejectsAWorkspaceTheCallerIsNotAMemberOf() {
        givenCurrentUserInWorkspace();

        Map<String, ThrowingCallable> entryPoints = Map.of(
            "aiAutoMemories", () -> aiAutoMemoryGraphQlController.aiAutoMemories(2L, 0, null, null),
            "aiAutoMemory", () -> aiAutoMemoryGraphQlController.aiAutoMemory(2L, 3L, 0, null),
            "aiAutoMemoryPrincipals", () -> aiAutoMemoryGraphQlController.aiAutoMemoryPrincipals(2L, 0),
            "updateAiAutoMemory", () -> aiAutoMemoryGraphQlController.updateAiAutoMemory(
                new AiAutoMemoryGraphQlController.UpdateAiAutoMemoryInput(
                    3L, 2L, 0, 0L, null, null, null, null, null)),
            "deleteAiAutoMemory", () -> aiAutoMemoryGraphQlController.deleteAiAutoMemory(2L, 3L, 0, null));

        entryPoints.forEach((entryPointName, entryPoint) -> assertThatThrownBy(entryPoint)
            .as(entryPointName)
            .isInstanceOf(AccessDeniedException.class));

        verifyNoInteractions(aiAutoMemoryService);
    }

    @Test
    void testUnknownEnvironmentIsRejected() {
        givenCurrentUserInWorkspace();

        assertThatThrownBy(() -> aiAutoMemoryGraphQlController.aiAutoMemories(1L, 7, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown environment");
    }

    private void givenCurrentUserInWorkspace() {
        User user = new User();

        user.setId(CURRENT_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace = new Workspace();

        workspace.setId(1L);

        when(workspaceFacade.getUserWorkspaces(CURRENT_USER_ID)).thenReturn(List.of(workspace));
    }

    private void givenEmbeddedDeployment(long deploymentId) {
        givenDeployments(deployment(deploymentId, 60L, "__EMBEDDED__customer-42"));
        givenProjects(project(60L, "__EMBEDDED__customer-42"));
    }

    private void givenDeployments(ProjectDeployment... projectDeployments) {
        when(projectDeploymentService.getProjectDeployments(any())).thenAnswer(invocation -> {
            List<Long> deploymentIds = invocation.getArgument(0);

            return Arrays.stream(projectDeployments)
                .filter(projectDeployment -> deploymentIds.contains(projectDeployment.getId()))
                .toList();
        });
    }

    private void givenProjects(Project... projects) {
        when(projectService.getProjects(any())).thenAnswer(invocation -> {
            List<Long> projectIds = invocation.getArgument(0);

            return Arrays.stream(projects)
                .filter(project -> projectIds.contains(project.getId()))
                .toList();
        });
    }

    private static ProjectDeployment deployment(long id, long projectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }

    private static Project project(long id, String name) {
        Project project = new Project();

        project.setId(id);
        project.setName(name);

        return project;
    }

    private static AiAutoMemory newMemory(AiAutoMemoryPrincipalType principalType, long principalId) {
        return new AiAutoMemory(new AiAutoMemoryOwner(1L, principalType, principalId, Environment.DEVELOPMENT));
    }

    private static AiAutoMemoryGraphQlController.AiAutoMemoryPrincipalInput principal(
        AiAutoMemoryPrincipalType principalType, long principalId) {

        return new AiAutoMemoryGraphQlController.AiAutoMemoryPrincipalInput(principalType, principalId);
    }

    private static void authenticateWith(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "tester", "password", List.of(new SimpleGrantedAuthority(authority))));
    }
}
