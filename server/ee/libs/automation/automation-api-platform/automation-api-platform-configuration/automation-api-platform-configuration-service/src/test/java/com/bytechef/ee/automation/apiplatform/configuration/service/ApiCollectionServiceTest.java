/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionServiceTest {

    private final ApiCollectionRepository apiCollectionRepository = mock(ApiCollectionRepository.class);
    private final ApiCollectionServiceImpl apiCollectionService = new ApiCollectionServiceImpl(apiCollectionRepository);

    @Test
    void testCreateAssignsUuidWhenMissing() {
        ApiCollection apiCollection = newApiCollection(null);

        assertThat(apiCollection.getUuid()).isNull();

        when(apiCollectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiCollection created = apiCollectionService.create(apiCollection);

        assertThat(created.getUuid()).isNotNull();
    }

    @Test
    void testCreateKeepsProvidedUuid() {
        UUID uuid = UUID.randomUUID();
        ApiCollection apiCollection = newApiCollection(uuid);

        when(apiCollectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(apiCollectionService.create(apiCollection)
            .getUuid()).isEqualTo(uuid);
    }

    @Test
    void testFetchApiCollectionByUuidAndEnvironmentDelegatesOrdinal() {
        UUID uuid = UUID.randomUUID();
        ApiCollection apiCollection = newApiCollection(uuid);

        when(apiCollectionRepository.findByUuidAndEnvironment(uuid, Environment.STAGING.ordinal()))
            .thenReturn(Optional.of(apiCollection));

        assertThat(apiCollectionService.fetchApiCollection(uuid, Environment.STAGING)).contains(apiCollection);
    }

    @Test
    void testExistsByNameAndEnvironmentDelegates() {
        when(
            apiCollectionRepository.existsByNameAndWorkspaceIdAndEnvironment(
                "billing", 5L, Environment.PRODUCTION.ordinal(), 9L))
                    .thenReturn(true);

        assertThat(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.PRODUCTION, 9L))
            .isTrue();
    }

    @Test
    void testExistsByNameAndEnvironmentRejectsNullName() {
        // A null name must fail the assertion here, before it ever reaches the repository's JdbcClient query —
        // List.of(name, ...) in CustomApiCollectionRepositoryImpl NPEs on a null element, which would otherwise
        // turn a clean validation failure into an opaque NPE.
        assertThatIllegalArgumentException()
            .isThrownBy(() -> apiCollectionService.existsByNameAndEnvironment(null, 5L, Environment.PRODUCTION, 9L));
    }

    private static ApiCollection newApiCollection(UUID uuid) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(1);
        apiCollection.setName("billing");
        apiCollection.setProjectDeploymentId(11L);
        apiCollection.setUuid(uuid);

        return apiCollection;
    }
}
