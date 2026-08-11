/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.repository.ApiConnectorRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorServiceTest {

    private ApiConnectorRepository apiConnectorRepository;
    private ApiConnectorServiceImpl apiConnectorService;

    @BeforeEach
    void beforeEach() {
        apiConnectorRepository = mock(ApiConnectorRepository.class);

        apiConnectorService = new ApiConnectorServiceImpl(apiConnectorRepository);

        when(apiConnectorRepository.save(any(ApiConnector.class))).thenAnswer(
            invocation -> invocation.getArgument(0));
    }

    /**
     * Re-importing a specification regenerates the compiled definition; the update must carry the new definition (and
     * the other regenerated fields) onto the persisted row, otherwise the runtime keeps serving stale endpoints.
     */
    @Test
    void testUpdateCopiesRegeneratedFields() {
        ApiConnector existingApiConnector = new ApiConnector();

        existingApiConnector.setId(1L);
        existingApiConnector.setName("petstore");
        existingApiConnector.setDefinition(new FileEntry("definition.json", "file:///old/definition.json"));
        existingApiConnector.setSpecification(new FileEntry("specification.yaml", "file:///old/specification.yaml"));
        existingApiConnector.setIcon("old-icon");
        existingApiConnector.setTitle("Old Title");

        when(apiConnectorRepository.findById(1L)).thenReturn(Optional.of(existingApiConnector));

        FileEntry newDefinition = new FileEntry("definition.json", "file:///new/definition.json");
        FileEntry newSpecification = new FileEntry("specification.yaml", "file:///new/specification.yaml");

        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setId(1L);
        apiConnector.setName("petstore");
        apiConnector.setDefinition(newDefinition);
        apiConnector.setSpecification(newSpecification);
        apiConnector.setDescription("New description");
        apiConnector.setIcon("new-icon");
        apiConnector.setTitle("New Title");

        ApiConnector updatedApiConnector = apiConnectorService.update(apiConnector);

        assertThat(updatedApiConnector.getDefinition()).isEqualTo(newDefinition);
        assertThat(updatedApiConnector.getSpecification()).isEqualTo(newSpecification);
        assertThat(updatedApiConnector.getDescription()).isEqualTo("New description");
        assertThat(updatedApiConnector.getIcon()).isEqualTo("new-icon");
        assertThat(updatedApiConnector.getTitle()).isEqualTo("New Title");
    }

    /**
     * Names are the lookup key of the runtime component registry, so an update must not be able to steal another row's
     * name.
     */
    @Test
    void testUpdateRejectsNameHeldByAnotherConnector() {
        ApiConnector otherApiConnector = new ApiConnector();

        otherApiConnector.setId(2L);
        otherApiConnector.setName("taken");

        when(apiConnectorRepository.findByNameAndConnectorVersion("taken", 1))
            .thenReturn(Optional.of(otherApiConnector));

        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setId(1L);
        apiConnector.setName("taken");
        apiConnector.setConnectorVersion(1);

        assertThatThrownBy(() -> apiConnectorService.update(apiConnector))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already exists");
    }
}
