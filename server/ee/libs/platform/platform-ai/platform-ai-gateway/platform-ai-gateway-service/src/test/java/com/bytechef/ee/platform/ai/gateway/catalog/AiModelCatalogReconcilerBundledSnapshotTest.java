/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reconciles against the real bundled models.dev snapshot — every other test in this package hand-builds
 * {@link com.bytechef.platform.ai.model.catalog.CatalogModel} instances with {@code new Limit(400000, null, 128000)},
 * which is exactly the shape of catalog data that never exercises the zero-context normalization Finding 1 of the
 * whole-branch review added. Only a reconcile against the shipped {@code models-dev-api.json} proves that every
 * provider the gateway maps completes its sweep — including the seven insertable, zero-context models the review
 * identified by hand (azure/gpt-image-1.5, groq/whisper-large-v3(-turbo), mistral/voxtral-mini-latest,
 * openai/gpt-image-1-mini, openai/chatgpt-image-latest, openai/gpt-image-1.5).
 *
 * <p>
 * {@link AiModelService} and {@link AiGatewayProviderService} stay mocked — this is not a database integration test —
 * but {@link ModelCatalog} and {@link ModelsDevSnapshotLoader} are real, so the parse runs against the actual shipped
 * file rather than a synthetic fixture.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiModelCatalogReconcilerBundledSnapshotTest {

    @Mock
    private AiModelService aiModelService;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    private static AiGatewayProvider enabledProvider(AiGatewayProviderType type, long id) {
        AiGatewayProvider provider = new AiGatewayProvider(type.name(), type, "sk-test");

        setId(provider, id);

        return provider;
    }

    private static void setId(Object target, long id) {
        try {
            Field field = target.getClass()
                .getDeclaredField("id");

            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Every {@link AiGatewayProviderType} the gateway maps, enabled with no existing rows, reconciled against the real
     * bundled snapshot. Asserts a plausible total insert count rather than an exact figure — the bundled snapshot is
     * refreshed independently of this test and an exact count would make every routine snapshot refresh fail this test
     * for no functional reason — and pins the specific zero-context models Finding 1 named by id, so a regression on
     * the normalization fails loudly rather than merely moving the aggregate count.
     */
    @Test
    void testReconcilesEveryMappedProviderAgainstBundledSnapshot() {
        ModelCatalog modelCatalog = new ModelCatalogImpl(new ModelsDevSnapshotLoader());

        AiModelCatalogReconciler reconciler = new AiModelCatalogReconcilerImpl(
            aiModelService, aiGatewayProviderService, modelCatalog);

        List<AiGatewayProvider> providers = new ArrayList<>();
        long id = 1L;

        for (AiGatewayProviderType type : AiGatewayProviderType.values()) {
            providers.add(enabledProvider(type, id++));
        }

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(providers);
        when(aiModelService.getModelsByProviderId(anyLong())).thenReturn(List.of());

        reconciler.reconcile();

        ArgumentCaptor<AiModel> captor = ArgumentCaptor.forClass(AiModel.class);

        org.mockito.Mockito.verify(aiModelService, org.mockito.Mockito.atLeastOnce())
            .create(captor.capture());

        List<AiModel> created = captor.getAllValues();

        assertThat(created)
            .as("reconciling all eight mapped providers against the real bundled snapshot should insert a "
                + "plausible number of rows, not silently skip half a provider")
            .hasSizeGreaterThan(100);

        Map<Long, List<String>> namesByProviderId = created.stream()
            .collect(Collectors.groupingBy(
                AiModel::getProviderId,
                Collectors.mapping(AiModel::getName, Collectors.toList())));

        assertThat(namesByProviderId)
            .as("every mapped provider must contribute at least one inserted row; a provider missing here is "
                + "exactly the silent per-provider drop Finding 1 described")
            .hasSize(AiGatewayProviderType.values().length);

        for (List<String> names : namesByProviderId.values()) {
            assertThat(names).isNotEmpty();
        }

        Map<String, AiModel> createdByName = created.stream()
            .collect(Collectors.toMap(AiModel::getName, model -> model, (first, second) -> first));

        // The seven insertable, zero-context models the review verified against the committed snapshot by hand.
        assertThat(createdByName).containsKeys(
            "gpt-image-1.5", "whisper-large-v3", "whisper-large-v3-turbo", "voxtral-mini-latest",
            "gpt-image-1-mini", "chatgpt-image-latest");

        assertThat(createdByName.get("whisper-large-v3")
            .getContextWindow())
                .as("a catalog-published context of 0 must normalize to null, not be skipped or thrown")
                .isNull();
    }
}
