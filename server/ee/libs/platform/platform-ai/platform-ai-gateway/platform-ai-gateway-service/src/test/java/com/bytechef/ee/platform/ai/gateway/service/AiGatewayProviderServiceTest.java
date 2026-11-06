/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayProviderRepository;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayProviderServiceTest {

    @Mock
    private AiGatewayChatModelFactory aiGatewayChatModelFactory;

    @Mock
    private AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory;

    @Mock
    private AiModelService aiModelService;

    @Mock
    private AiGatewayProviderRepository aiGatewayProviderRepository;

    private AiGatewayProviderService aiGatewayProviderService;

    @BeforeEach
    void setUp() {
        aiGatewayProviderService = new AiGatewayProviderServiceImpl(
            aiGatewayChatModelFactory, aiGatewayEmbeddingModelFactory, aiModelService,
            aiGatewayProviderRepository);
    }

    @Test
    void testCreateRejectsProviderWithAssignedId() {
        AiGatewayProvider provider = newProviderWithId(42L);

        assertThatThrownBy(() -> aiGatewayProviderService.create(provider))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'id' must be null");
    }

    @Test
    void testDeleteEvictsBothFactoryCachesAndCascadesModelDeletion() {
        AiModel firstModel = newModelWithId(1L);
        AiModel secondModel = newModelWithId(2L);

        when(aiModelService.getModelsByProviderId(50L)).thenReturn(List.of(firstModel, secondModel));

        aiGatewayProviderService.delete(50L);

        verify(aiGatewayChatModelFactory).evict(50L);
        verify(aiGatewayEmbeddingModelFactory).evict(50L);
        verify(aiModelService).delete(1L);
        verify(aiModelService).delete(2L);
        verify(aiGatewayProviderRepository).deleteById(50L);
    }

    @Test
    void testGetProviderThrowsWhenMissing() {
        when(aiGatewayProviderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiGatewayProviderService.getProvider(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Provider not found: 999");
    }

    @Test
    void testGetEnabledProvidersDelegatesToEnabledFinder() {
        AiGatewayProvider enabledProvider =
            new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        when(aiGatewayProviderRepository.findAllByEnabled(true)).thenReturn(List.of(enabledProvider));

        List<AiGatewayProvider> providers = aiGatewayProviderService.getEnabledProviders();

        assertThat(providers).containsExactly(enabledProvider);

        verify(aiGatewayProviderRepository).findAllByEnabled(true);
    }

    @Test
    void testCreateConnectedUserProviderBindsTheConnectedUserAtCreation() {
        AiGatewayProvider provider = new AiGatewayProvider("customer", AiGatewayProviderType.OPENAI, "sk-customer");

        when(aiGatewayProviderRepository.findByConnectedUserIdAndType(42L, AiGatewayProviderType.OPENAI.ordinal()))
            .thenReturn(Optional.empty());
        when(aiGatewayProviderRepository.save(provider)).thenReturn(provider);

        AiGatewayProvider created = aiGatewayProviderService.createConnectedUserProvider(provider, 42L);

        assertThat(created.getConnectedUserId()).isEqualTo(42L);
    }

    @Test
    void testCreateConnectedUserProviderRefusesASecondProviderOfTheSameTypeEvenWhenDisabled() {
        AiGatewayProvider existing = newProviderWithId(7L);

        existing.setEnabled(false);

        when(aiGatewayProviderRepository.findByConnectedUserIdAndType(42L, AiGatewayProviderType.OPENAI.ordinal()))
            .thenReturn(Optional.of(existing));

        AiGatewayProvider provider = new AiGatewayProvider("customer", AiGatewayProviderType.OPENAI, "sk-customer");

        assertThatThrownBy(() -> aiGatewayProviderService.createConnectedUserProvider(provider, 42L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Connected user 42 already has a provider of type OPENAI (7); delete it first");

        verify(aiGatewayProviderRepository, never()).save(any());
    }

    @Test
    void testCreateConnectedUserProviderRefusesAWorkspaceScopedProvider() {
        AiGatewayProvider provider = new AiGatewayProvider("customer", AiGatewayProviderType.OPENAI, "sk-customer");

        provider.setWorkspaceId(9L);

        assertThatThrownBy(() -> aiGatewayProviderService.createConnectedUserProvider(provider, 42L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A connected user's provider cannot belong to a workspace");

        verify(aiGatewayProviderRepository, never()).save(any());
    }

    @Test
    void testDisableByConnectedUserIdDisablesEveryProviderAndEvictsBothCaches() {
        AiGatewayProvider firstProvider = newProviderWithId(7L);
        AiGatewayProvider secondProvider = newProviderWithId(8L);

        firstProvider.setConnectedUserId(42L);
        secondProvider.setConnectedUserId(42L);

        when(aiGatewayProviderRepository.findAllByConnectedUserId(42L))
            .thenReturn(List.of(firstProvider, secondProvider));

        aiGatewayProviderService.disableByConnectedUserId(42L);

        ArgumentCaptor<AiGatewayProvider> providerCaptor = ArgumentCaptor.forClass(AiGatewayProvider.class);

        verify(aiGatewayProviderRepository, org.mockito.Mockito.times(2)).save(providerCaptor.capture());

        assertThat(providerCaptor.getAllValues())
            .allSatisfy(provider -> {
                assertThat(provider.isEnabled()).isFalse();
                assertThat(provider.getConnectedUserId()).isEqualTo(42L);
            });

        verify(aiGatewayChatModelFactory).evict(7L);
        verify(aiGatewayChatModelFactory).evict(8L);
        verify(aiGatewayEmbeddingModelFactory).evict(7L);
        verify(aiGatewayEmbeddingModelFactory).evict(8L);
    }

    private static AiGatewayProvider newProviderWithId(long id) {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-test");

        setIdViaReflection(provider, id);

        return provider;
    }

    private static AiModel newModelWithId(long id) {
        AiModel model = new AiModel(1L, "gpt-4");

        setIdViaReflection(model, id);

        return model;
    }

    private static void setIdViaReflection(Object target, long id) {
        try {
            Field idField = target.getClass()
                .getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }
    }
}
