/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class IntegrationInstanceServiceTest {

    @Mock
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Test
    void testGetIntegrationInstanceByComponentNamesRejectsEmptyComponentNames() {
        IntegrationInstanceService integrationInstanceService = new IntegrationInstanceServiceImpl(
            integrationInstanceRepository);

        assertThatThrownBy(
            () -> integrationInstanceService.getIntegrationInstance(1L, List.of(), Environment.PRODUCTION))
                .isInstanceOf(NoSuchElementException.class);

        verify(integrationInstanceRepository, never()).findFirstByConnectedUserIdIdAndComponentNamesAndEnvironment(
            anyLong(), anyList(), anyInt());
    }

    @Test
    void testGetIntegrationInstanceByComponentNamesLowercasesComponentNames() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        when(integrationInstanceRepository.findFirstByConnectedUserIdIdAndComponentNamesAndEnvironment(
            1L, List.of("hubspot"), Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(integrationInstance));

        IntegrationInstanceService integrationInstanceService = new IntegrationInstanceServiceImpl(
            integrationInstanceRepository);

        assertThat(integrationInstanceService.getIntegrationInstance(1L, List.of("HubSpot"), Environment.PRODUCTION))
            .isSameAs(integrationInstance);
    }
}
