/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the update guardrails (optimistic version check, name-conflict rejection) and the orphaned-file sweep of
 * {@link ApiConnectorFacadeImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorFacadeUpdateTest {

    private ApiConnectorFileStorage apiConnectorFileStorage;
    private ApiConnectorService apiConnectorService;
    private ApiConnectorFacadeImpl apiConnectorFacade;

    @BeforeEach
    void beforeEach() {
        apiConnectorFileStorage = mock(ApiConnectorFileStorage.class);
        apiConnectorService = mock(ApiConnectorService.class);

        apiConnectorFacade = new ApiConnectorFacadeImpl(null, apiConnectorFileStorage, apiConnectorService);
    }

    @Test
    void testUpdateApiConnectorRejectsStaleVersion() {
        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setId(1L);
        apiConnector.setName("petstore");
        apiConnector.setVersion(3);

        when(apiConnectorService.getApiConnector(1L)).thenReturn(apiConnector);

        assertThatThrownBy(() -> apiConnectorFacade.updateApiConnector(1L, "petstore", null, "openapi: 3.0.0", 2))
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

    @Test
    void testDeleteOrphanedFilesDeletesOnlyUnreferencedBlobs() {
        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setId(1L);
        apiConnector.setName("petstore");
        apiConnector.setDefinition(new FileEntry("definition.json", "file:///definitions/referenced.json"));
        apiConnector.setSpecification(new FileEntry("specification.yaml", "file:///specifications/referenced.yaml"));

        FileEntry orphanedDefinition = new FileEntry("definition.json", "file:///definitions/orphaned.json");
        FileEntry orphanedSpecification = new FileEntry("specification.yaml", "file:///specifications/orphaned.yaml");

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
        verify(apiConnectorFileStorage, never()).deleteApiConnectorSpecification(apiConnector.getSpecification());
    }
}
