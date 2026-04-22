/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayProviderRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiGatewayProviderServiceImpl}. Covers the three behaviors most likely to regress: the
 * {@code Validate.isTrue(id == null)} guard on create, the cascading evict/model-delete on delete, and the "getProvider
 * throws when missing" contract relied on by the facade-layer workspace-ownership checks.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayProviderServiceTest {

    @Mock
    private AiGatewayChatModelFactory aiGatewayChatModelFactory;

    @Mock
    private AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory;

    @Mock
    private AiGatewayModelService aiGatewayModelService;

    @Mock
    private AiGatewayProviderRepository aiGatewayProviderRepository;

    private AiGatewayProviderService aiGatewayProviderService;

    @BeforeEach
    void setUp() {
        aiGatewayProviderService = new AiGatewayProviderServiceImpl(
            aiGatewayChatModelFactory, aiGatewayEmbeddingModelFactory, aiGatewayModelService,
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
        AiGatewayModel firstModel = newModelWithId(1L);
        AiGatewayModel secondModel = newModelWithId(2L);

        when(aiGatewayModelService.getModelsByProviderId(50L)).thenReturn(List.of(firstModel, secondModel));

        aiGatewayProviderService.delete(50L);

        // Both cache evictions must fire before the repo delete — without them a deleted provider's
        // encrypted api key stays resident in the chat/embedding caches and is used on subsequent requests.
        verify(aiGatewayChatModelFactory).evict(50L);
        verify(aiGatewayEmbeddingModelFactory).evict(50L);
        verify(aiGatewayModelService).delete(1L);
        verify(aiGatewayModelService).delete(2L);
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

    private static AiGatewayProvider newProviderWithId(long id) {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-test");

        setIdViaReflection(provider, id);

        return provider;
    }

    private static AiGatewayModel newModelWithId(long id) {
        AiGatewayModel model = new AiGatewayModel(1L, "gpt-4");

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
