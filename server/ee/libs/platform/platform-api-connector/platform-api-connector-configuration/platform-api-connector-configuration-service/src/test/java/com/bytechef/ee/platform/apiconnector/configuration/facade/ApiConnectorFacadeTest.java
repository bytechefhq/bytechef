/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.dto.GenerateSpecificationRequestDTO;
import com.bytechef.ee.platform.apiconnector.configuration.generator.OpenApiComponentDefinitionFactory;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorAiService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.ee.platform.apiconnector.configuration.service.OpenApiSpecificationGenerator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorFacadeTest {

    private static final String STORED_DEFINITION = """
        {"name": "petstore", "title": "Petstore", "description": "Old description", "version": 1, "actions": []}
        """;

    private ApiConnectorAiService apiConnectorAiService;
    private ApiConnectorFileStorage apiConnectorFileStorage;
    private ApiConnectorGenerationJobService apiConnectorGenerationJobService;
    private ApiConnectorService apiConnectorService;
    private OpenApiSpecificationGenerator openApiSpecificationGenerator;
    private ApiConnectorFacadeImpl apiConnectorFacade;

    @BeforeEach
    void beforeEach() {
        apiConnectorAiService = mock(ApiConnectorAiService.class);
        apiConnectorFileStorage = mock(ApiConnectorFileStorage.class);
        apiConnectorGenerationJobService = mock(ApiConnectorGenerationJobService.class);
        apiConnectorService = mock(ApiConnectorService.class);
        openApiSpecificationGenerator = mock(OpenApiSpecificationGenerator.class);

        apiConnectorFacade = new ApiConnectorFacadeImpl(
            apiConnectorAiService, apiConnectorFileStorage, apiConnectorGenerationJobService, apiConnectorService,
            openApiSpecificationGenerator);
    }

    @Nested
    class ConvertComponentName {

        @Test
        void testConvertComponentNameNormalizes() {
            assertEquals("petstore", ApiConnectorFacadeImpl.convertComponentName("Pet Store"));
            assertEquals("pet.store", ApiConnectorFacadeImpl.convertComponentName("pet-store"));
        }

        @Test
        void testConvertComponentNameRejectsPathTraversal() {
            assertThrows(
                ConfigurationException.class,
                () -> ApiConnectorFacadeImpl.convertComponentName("../../etc/passwd"));
            assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName(".."));
            assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("a/b"));
            assertThrows(
                ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("a\\b"));
        }

        @Test
        void testConvertComponentNameRejectsDottedTraversal() {
            assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("a--b"));
            assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName(".lead"));
            assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("trail."));
        }

        @Test
        void testConvertComponentNameRejectsBlank() {
            assertThrows(ConfigurationException.class, () -> ApiConnectorFacadeImpl.convertComponentName("   "));
        }
    }

    @Nested
    class UpdateApiConnector {

        @Test
        void testUpdateApiConnectorRejectsStaleVersion() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");
            apiConnector.setVersion(3);

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);

            assertThatThrownBy(
                () -> apiConnectorFacade.updateApiConnector(1L, "petstore", null, "openapi: 3.0.0", 2))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("modified by someone else");
        }

        @Test
        void testUpdateApiConnectorRejectsNameHeldByAnotherConnector() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");
            apiConnector.setVersion(1);

            ApiConnector otherApiConnector = new ApiConnector();

            otherApiConnector.setId(2L);
            otherApiConnector.setName("taken");

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.fetchApiConnector("taken", 1)).thenReturn(Optional.of(otherApiConnector));

            assertThatThrownBy(() -> apiConnectorFacade.updateApiConnector(1L, "taken", null, "openapi: 3.0.0", 1))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("already exists");
        }
    }

    @Nested
    class DeleteOrphanedFiles {

        @Test
        void testDeleteOrphanedFilesDeletesOnlyUnreferencedBlobs() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");
            apiConnector.setDefinition(new FileEntry("definition.json", "file:///definitions/referenced.json"));
            apiConnector.setSpecification(
                new FileEntry("specification.yaml", "file:///specifications/referenced.yaml"));

            FileEntry orphanedDefinition = new FileEntry("definition.json", "file:///definitions/orphaned.json");
            FileEntry orphanedSpecification = new FileEntry(
                "specification.yaml", "file:///specifications/orphaned.yaml");

            when(apiConnectorService.getApiConnectors()).thenReturn(List.of(apiConnector));
            when(apiConnectorFileStorage.getApiConnectorDefinitionFileEntries())
                .thenReturn(Set.of(apiConnector.getDefinition(), orphanedDefinition));
            when(apiConnectorFileStorage.getApiConnectorSpecificationFileEntries())
                .thenReturn(Set.of(apiConnector.getSpecification(), orphanedSpecification));

            int deletedFiles = apiConnectorFacade.deleteOrphanedFiles();

            assertThat(deletedFiles).isEqualTo(2);

            verify(apiConnectorFileStorage).deleteApiConnectorDefinition(orphanedDefinition);
            verify(apiConnectorFileStorage).deleteApiConnectorSpecification(orphanedSpecification);
            verify(apiConnectorFileStorage, never()).deleteApiConnectorDefinition(apiConnector.getDefinition());
            verify(apiConnectorFileStorage, never())
                .deleteApiConnectorSpecification(apiConnector.getSpecification());
        }
    }

    @Nested
    class UpdateApiConnectorMetadata {

        @Test
        void testUpdateApiConnectorMetadataAppliesOnlyNonNullFields() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");
            apiConnector.setTitle("Petstore");
            apiConnector.setDescription("Old description");
            apiConnector.setConnectorVersion(1);

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.update(apiConnector)).thenReturn(apiConnector);

            ApiConnector updatedApiConnector = apiConnectorFacade.updateApiConnectorMetadata(
                1L, null, "Pet Store", null, "icon.svg", 2);

            assertThat(updatedApiConnector.getName()).isEqualTo("petstore");
            assertThat(updatedApiConnector.getTitle()).isEqualTo("Pet Store");
            assertThat(updatedApiConnector.getDescription()).isEqualTo("Old description");
            assertThat(updatedApiConnector.getIcon()).isEqualTo("icon.svg");
            assertThat(updatedApiConnector.getConnectorVersion()).isEqualTo(2);

            verify(apiConnectorService).update(apiConnector);
        }

        @Test
        void testUpdateApiConnectorMetadataNormalizesName() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.fetchApiConnector("pet.store", 1)).thenReturn(Optional.empty());
            when(apiConnectorService.update(apiConnector)).thenReturn(apiConnector);

            ApiConnector updatedApiConnector = apiConnectorFacade.updateApiConnectorMetadata(
                1L, "Pet-Store", null, null, null, null);

            assertThat(updatedApiConnector.getName()).isEqualTo("pet.store");
        }

        @Test
        void testUpdateApiConnectorMetadataRejectsInvalidName() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);

            assertThatThrownBy(
                () -> apiConnectorFacade.updateApiConnectorMetadata(1L, "../../etc/passwd", null, null, null, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Invalid API connector name");

            assertThat(apiConnector.getName()).isEqualTo("petstore");

            verify(apiConnectorService, never()).update(any());
        }

        @Test
        void testUpdateApiConnectorMetadataRejectsNameHeldByAnotherConnector() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");

            ApiConnector otherApiConnector = new ApiConnector();

            otherApiConnector.setId(2L);
            otherApiConnector.setName("taken");

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.fetchApiConnector("taken", 1)).thenReturn(Optional.of(otherApiConnector));

            assertThatThrownBy(
                () -> apiConnectorFacade.updateApiConnectorMetadata(1L, "taken", null, null, null, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("already exists");

            verify(apiConnectorService, never()).update(any());
        }

        @Test
        void testUpdateApiConnectorMetadataRewritesTheStoredDefinitionOnRename() {
            FileEntry oldDefinition = new FileEntry("definition.json", "file:///definitions/old.json");
            FileEntry newDefinition = new FileEntry("definition.json", "file:///definitions/new.json");

            ApiConnector apiConnector = createApiConnectorWithDefinition(oldDefinition);

            String expectedDefinition = OpenApiComponentDefinitionFactory.updateComponentDefinitionJson(
                STORED_DEFINITION, "pet.store", "Petstore", "Old description", 1);

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.fetchApiConnector("pet.store", 1)).thenReturn(Optional.empty());
            when(apiConnectorService.update(apiConnector)).thenReturn(apiConnector);
            when(apiConnectorFileStorage.readApiConnectorDefinition(oldDefinition)).thenReturn(STORED_DEFINITION);
            when(apiConnectorFileStorage.storeApiConnectorDefinition("definition.json", expectedDefinition))
                .thenReturn(newDefinition);

            ApiConnector updatedApiConnector = apiConnectorFacade.updateApiConnectorMetadata(
                1L, "pet-store", null, null, null, null);

            assertThat(updatedApiConnector.getName()).isEqualTo("pet.store");
            assertThat(updatedApiConnector.getDefinition()).isEqualTo(newDefinition);
            assertThat(expectedDefinition).contains("\"pet.store\"")
                .doesNotContain("\"petstore\"");

            verify(apiConnectorFileStorage).storeApiConnectorDefinition("definition.json", expectedDefinition);
            verify(apiConnectorFileStorage).deleteApiConnectorDefinition(oldDefinition);
        }

        @Test
        void testUpdateApiConnectorMetadataRewritesTheStoredDefinitionOnTitleAndVersionChange() {
            FileEntry oldDefinition = new FileEntry("definition.json", "file:///definitions/old.json");
            FileEntry newDefinition = new FileEntry("definition.json", "file:///definitions/new.json");

            ApiConnector apiConnector = createApiConnectorWithDefinition(oldDefinition);

            String expectedDefinition = OpenApiComponentDefinitionFactory.updateComponentDefinitionJson(
                STORED_DEFINITION, "petstore", "Pet Store", "New description", 2);

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.update(apiConnector)).thenReturn(apiConnector);
            when(apiConnectorFileStorage.readApiConnectorDefinition(oldDefinition)).thenReturn(STORED_DEFINITION);
            when(apiConnectorFileStorage.storeApiConnectorDefinition("definition.json", expectedDefinition))
                .thenReturn(newDefinition);

            ApiConnector updatedApiConnector = apiConnectorFacade.updateApiConnectorMetadata(
                1L, null, "Pet Store", "New description", null, 2);

            assertThat(updatedApiConnector.getDefinition()).isEqualTo(newDefinition);

            verify(apiConnectorFileStorage).deleteApiConnectorDefinition(oldDefinition);
        }

        @Test
        void testUpdateApiConnectorMetadataKeepsTheStoredDefinitionOnIconOnlyChange() {
            FileEntry oldDefinition = new FileEntry("definition.json", "file:///definitions/old.json");

            ApiConnector apiConnector = createApiConnectorWithDefinition(oldDefinition);

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.update(apiConnector)).thenReturn(apiConnector);

            ApiConnector updatedApiConnector = apiConnectorFacade.updateApiConnectorMetadata(
                1L, null, null, null, "icon.svg", null);

            assertThat(updatedApiConnector.getDefinition()).isEqualTo(oldDefinition);

            verify(apiConnectorFileStorage, never()).storeApiConnectorDefinition(any(), any());
            verify(apiConnectorFileStorage, never()).deleteApiConnectorDefinition(any());
        }

        @Test
        void testUpdateApiConnectorMetadataDoesNotStoreADefinitionWhenTheNameIsTaken() {
            FileEntry oldDefinition = new FileEntry("definition.json", "file:///definitions/old.json");

            ApiConnector apiConnector = createApiConnectorWithDefinition(oldDefinition);

            ApiConnector otherApiConnector = new ApiConnector();

            otherApiConnector.setId(2L);

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.fetchApiConnector("taken", 1)).thenReturn(Optional.of(otherApiConnector));

            assertThatThrownBy(
                () -> apiConnectorFacade.updateApiConnectorMetadata(1L, "taken", null, null, null, null))
                    .isInstanceOf(ConfigurationException.class);

            verify(apiConnectorFileStorage, never()).storeApiConnectorDefinition(any(), any());
            verify(apiConnectorFileStorage, never()).deleteApiConnectorDefinition(any());
        }

        private ApiConnector createApiConnectorWithDefinition(FileEntry definition) {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");
            apiConnector.setTitle("Petstore");
            apiConnector.setDescription("Old description");
            apiConnector.setConnectorVersion(1);
            apiConnector.setDefinition(definition);

            return apiConnector;
        }
    }

    @Nested
    class EnableApiConnector {

        @Test
        void testEnableApiConnectorDelegatesToService() {
            apiConnectorFacade.enableApiConnector(1L, true);

            verify(apiConnectorService).enableApiConnector(1L, true);
        }
    }

    @Nested
    class GenerationJobs {

        @Test
        void testStartGenerateFromDocumentationPreviewCreatesJobAndStartsGeneration() {
            ApiConnectorGenerationJob apiConnectorGenerationJob = new ApiConnectorGenerationJob(
                "job-1", "https://docs.example.com");

            when(apiConnectorGenerationJobService.create("https://docs.example.com"))
                .thenReturn(apiConnectorGenerationJob);

            ApiConnectorGenerationJob startedApiConnectorGenerationJob =
                apiConnectorFacade.startGenerateFromDocumentationPreview("https://docs.example.com", "Only pets", 3);

            assertThat(startedApiConnectorGenerationJob).isSameAs(apiConnectorGenerationJob);

            verify(apiConnectorAiService).generateOpenApiSpecificationAsync(
                "job-1", "https://docs.example.com", "Only pets", 3);
        }

        @Test
        void testStartGenerateFromDocumentationPreviewDefaultsToSinglePage() {
            ApiConnectorGenerationJob apiConnectorGenerationJob = new ApiConnectorGenerationJob(
                "job-1", "https://docs.example.com");

            when(apiConnectorGenerationJobService.create("https://docs.example.com"))
                .thenReturn(apiConnectorGenerationJob);

            apiConnectorFacade.startGenerateFromDocumentationPreview("https://docs.example.com", null, null);
            apiConnectorFacade.startGenerateFromDocumentationPreview("https://docs.example.com", null, 0);

            verify(apiConnectorAiService, times(2))
                .generateOpenApiSpecificationAsync("job-1", "https://docs.example.com", null, 1);
        }

        @Test
        void testStartGenerateFromDocumentationPreviewRejectsMissingAiService() {
            ApiConnectorFacadeImpl apiConnectorFacadeWithoutAi = new ApiConnectorFacadeImpl(
                null, apiConnectorFileStorage, apiConnectorGenerationJobService, apiConnectorService,
                openApiSpecificationGenerator);

            assertThatThrownBy(() -> apiConnectorFacadeWithoutAi.startGenerateFromDocumentationPreview(
                "https://docs.example.com", null, 1))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("AI service is not configured");

            verifyNoInteractions(apiConnectorGenerationJobService);
        }

        @Test
        void testCancelGenerationJobRequestsCancellation() {
            when(apiConnectorGenerationJobService.requestCancellation("job-1")).thenReturn(true);

            assertThat(apiConnectorFacade.cancelGenerationJob("job-1")).isTrue();
        }

        @Test
        void testFetchGenerationJobReturnsJob() {
            ApiConnectorGenerationJob apiConnectorGenerationJob = new ApiConnectorGenerationJob(
                "job-1", "https://docs.example.com");

            when(apiConnectorGenerationJobService.get("job-1")).thenReturn(Optional.of(apiConnectorGenerationJob));

            assertThat(apiConnectorFacade.fetchGenerationJob("job-1")).contains(apiConnectorGenerationJob);
        }
    }

    @Nested
    class GenerateSpecification {

        @Test
        void testGenerateSpecificationDelegatesToGenerator() {
            GenerateSpecificationRequestDTO generateSpecificationRequestDTO = new GenerateSpecificationRequestDTO(
                "petstore", "https://petstore.example.com", List.of());

            when(openApiSpecificationGenerator.generate(generateSpecificationRequestDTO))
                .thenReturn("openapi: 3.0.0");

            assertThat(apiConnectorFacade.generateSpecification(generateSpecificationRequestDTO))
                .isEqualTo("openapi: 3.0.0");
        }
    }

    @Nested
    class Authorization {

        private ApiConnectorFacade securedApiConnectorFacade;

        @BeforeEach
        void beforeEachCreateSecuredFacade() {
            ProxyFactory proxyFactory = new ProxyFactory(apiConnectorFacade);

            proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());

            securedApiConnectorFacade = (ApiConnectorFacade) proxyFactory.getProxy();
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testEnableApiConnectorRejectsNonAdmin() {
            authenticate("ROLE_USER");

            assertThatThrownBy(() -> securedApiConnectorFacade.enableApiConnector(1L, true))
                .isInstanceOf(AccessDeniedException.class);

            verify(apiConnectorService, never()).enableApiConnector(1L, true);
        }

        @Test
        void testEnableApiConnectorAllowsAdmin() {
            authenticate("ROLE_ADMIN");

            securedApiConnectorFacade.enableApiConnector(1L, false);

            verify(apiConnectorService).enableApiConnector(1L, false);
        }

        @Test
        void testUpdateApiConnectorMetadataRejectsNonAdmin() {
            authenticate("ROLE_USER");

            assertThatThrownBy(
                () -> securedApiConnectorFacade.updateApiConnectorMetadata(1L, null, "Title", null, null, null))
                    .isInstanceOf(AccessDeniedException.class);

            verify(apiConnectorService, never()).update(any());
        }

        @Test
        void testUpdateApiConnectorMetadataAllowsAdmin() {
            ApiConnector apiConnector = new ApiConnector();

            apiConnector.setId(1L);
            apiConnector.setName("petstore");

            when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);
            when(apiConnectorService.update(apiConnector)).thenReturn(apiConnector);

            authenticate("ROLE_ADMIN");

            ApiConnector updatedApiConnector = securedApiConnectorFacade.updateApiConnectorMetadata(
                1L, null, "Title", null, null, null);

            assertThat(updatedApiConnector.getTitle()).isEqualTo("Title");
        }

        @Test
        void testStartGenerateFromDocumentationPreviewRejectsNonAdmin() {
            authenticate("ROLE_USER");

            assertThatThrownBy(() -> securedApiConnectorFacade.startGenerateFromDocumentationPreview(
                "https://docs.example.com", null, 1))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(apiConnectorGenerationJobService, apiConnectorAiService);
        }

        @Test
        void testCancelGenerationJobRejectsNonAdmin() {
            authenticate("ROLE_USER");

            assertThatThrownBy(() -> securedApiConnectorFacade.cancelGenerationJob("job-1"))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(apiConnectorGenerationJobService);
        }

        @Test
        void testGenerateSpecificationRejectsNonAdmin() {
            authenticate("ROLE_USER");

            GenerateSpecificationRequestDTO generateSpecificationRequestDTO = new GenerateSpecificationRequestDTO(
                "petstore", null, List.of());

            assertThatThrownBy(() -> securedApiConnectorFacade.generateSpecification(generateSpecificationRequestDTO))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(openApiSpecificationGenerator);
        }

        @Test
        void testGenerationOperationsAllowAdmin() {
            ApiConnectorGenerationJob apiConnectorGenerationJob = new ApiConnectorGenerationJob(
                "job-1", "https://docs.example.com");

            when(apiConnectorGenerationJobService.create("https://docs.example.com"))
                .thenReturn(apiConnectorGenerationJob);
            when(apiConnectorGenerationJobService.requestCancellation("job-1")).thenReturn(true);

            authenticate("ROLE_ADMIN");

            assertThat(securedApiConnectorFacade.startGenerateFromDocumentationPreview(
                "https://docs.example.com", null, 1)).isSameAs(apiConnectorGenerationJob);
            assertThat(securedApiConnectorFacade.cancelGenerationJob("job-1")).isTrue();

            securedApiConnectorFacade.generateSpecification(
                new GenerateSpecificationRequestDTO("petstore", null, List.of()));

            verify(openApiSpecificationGenerator).generate(any());
        }

        @Test
        void testFetchGenerationJobAllowsNonAdminLikeOtherReads() {
            ApiConnectorGenerationJob apiConnectorGenerationJob = new ApiConnectorGenerationJob(
                "job-1", "https://docs.example.com");

            when(apiConnectorGenerationJobService.get("job-1")).thenReturn(Optional.of(apiConnectorGenerationJob));

            authenticate("ROLE_USER");

            assertThat(securedApiConnectorFacade.fetchGenerationJob("job-1")).contains(apiConnectorGenerationJob);
        }

        private void authenticate(String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("user", "password", authority));
        }
    }
}
