/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.CatalogModel.Status;
import com.bytechef.platform.ai.model.catalog.Cost;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiModelCatalogReconcilerTest {

    @Mock
    private AiModelService aiModelService;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private ModelCatalog modelCatalog;

    private AiModelCatalogReconciler reconciler;

    private static CatalogModel catalogModel(String id, Status status, Modality outputModality) {
        return new CatalogModel(
            id, id, null, null, false, false, true, false, true, false, null, null, null, status,
            new Modalities(List.of(Modality.TEXT), List.of(outputModality)), new Limit(400000, null, 128000),
            new Cost(
                new BigDecimal("1.25"), BigDecimal.TEN, null, null, null, null, null, List.of()));
    }

    private static AiGatewayProvider enabledProvider() {
        AiGatewayProvider provider = new AiGatewayProvider("OpenAI", AiGatewayProviderType.OPENAI, "sk-test");

        setId(provider, 7L);

        return provider;
    }

    private static AiGatewayProvider disabledProvider() {
        AiGatewayProvider provider = new AiGatewayProvider("OpenAI", AiGatewayProviderType.OPENAI, "sk-test");

        provider.setEnabled(false);
        setId(provider, 7L);

        return provider;
    }

    private static AiModel existingRow(String name, boolean pinned) {
        AiModel model = new AiModel(7L, name);

        model.setCatalogPinned(pinned);
        setId(model, 42L);

        return model;
    }

    private static AiModel existingRow(long providerId, String name, long id, boolean pinned) {
        AiModel model = new AiModel(providerId, name);

        model.setCatalogPinned(pinned);
        setId(model, id);

        return model;
    }

    private static void setId(Object target, Long id) {
        try {
            Field field = target.getClass()
                .getDeclaredField("id");

            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @BeforeEach
    void setUp() {
        reconciler = new AiModelCatalogReconcilerImpl(
            aiModelService, aiGatewayProviderService, modelCatalog);
    }

    @Test
    void testInsertsMissingModelWithCatalogValues() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        ArgumentCaptor<AiModel> captor = ArgumentCaptor.forClass(AiModel.class);

        verify(aiModelService).create(captor.capture());

        AiModel created = captor.getValue();

        assertThat(created.getName()).isEqualTo("gpt-5.1");
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getContextWindow()).isEqualTo(400000);
        assertThat(created.getInputCostPerMTokens()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(created.getOutputCostPerMTokens()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(created.getCapabilities()).isEqualTo("temperature,tool_call");
    }

    @Test
    void testUpdatesUnpinnedExistingRow() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("gpt-5.1", false)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiModelService).updateFromCatalog(any());
        verify(aiModelService, never()).create(any());
    }

    @Test
    void testSkipsPinnedRow() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("gpt-5.1", true)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiModelService, never()).updateFromCatalog(any());
        verify(aiModelService, never()).create(any());
    }

    @Test
    void testSkipsUncataloguedRowSilently() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));

        lenient().when(aiModelService.getModelsByProviderId(7L))
            .thenReturn(
                List.of(existingRow("ft:gpt-4o:acme:x", false)));
        when(modelCatalog.getModels("openai")).thenReturn(List.of());

        reconciler.reconcile();

        verify(aiModelService, never()).updateFromCatalog(any());
        verify(aiModelService, never()).create(any());
    }

    @Test
    void testSkipsDisabledProvider() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        reconciler.reconcile();

        verify(aiModelService, never()).getModelsByProviderId(anyLong());
        verify(aiModelService, never()).create(any());
    }

    @Test
    void testDoesNotInsertDeprecatedModel() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-4", Status.DEPRECATED, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiModelService, never()).create(any());
    }

    @Test
    void testStillUpdatesDeprecatedModelThatAlreadyHasARow() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("gpt-4", false)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-4", Status.DEPRECATED, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiModelService).updateFromCatalog(any());
    }

    @Test
    void testDoesNotInsertNonTextOutputModel() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("dall-e-3", Status.ACTIVE, Modality.IMAGE)));

        reconciler.reconcile();

        verify(aiModelService, never()).create(any());
    }

    @Test
    void testInsertsZeroContextModelAsNullContextWindow() {
        CatalogModel zeroContext = new CatalogModel(
            "whisper-large-v3", "whisper-large-v3", null, null, false, false, false, false, false, false, null, null,
            null, Status.ACTIVE, new Modalities(List.of(Modality.AUDIO), List.of(Modality.TEXT)),
            new Limit(0, null, null), null);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(List.of(zeroContext));

        reconciler.reconcile();

        ArgumentCaptor<AiModel> captor = ArgumentCaptor.forClass(AiModel.class);

        verify(aiModelService).create(captor.capture());

        AiModel created = captor.getValue();

        assertThat(created.getName()).isEqualTo("whisper-large-v3");
        assertThat(created.getContextWindow()).isNull();
    }

    @Test
    void testSecondReconcileOfZeroContextModelWritesNothing() {
        CatalogModel zeroContext = new CatalogModel(
            "whisper-large-v3", "whisper-large-v3", null, null, false, false, false, false, false, false, null, null,
            null, Status.ACTIVE, new Modalities(List.of(Modality.AUDIO), List.of(Modality.TEXT)),
            new Limit(0, null, null), null);

        AiModel alreadyNormalized = existingRow("whisper-large-v3", false);

        alreadyNormalized.setCapabilities(CapabilitiesEncoder.encode(zeroContext));
        alreadyNormalized.setContextWindow(null);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of(alreadyNormalized));
        when(modelCatalog.getModels("openai")).thenReturn(List.of(zeroContext));

        reconciler.reconcile();

        verify(aiModelService, never()).updateFromCatalog(any());
    }

    @Test
    void testOneThrowingModelDoesNotBlockLaterModelsInSameProvider() {
        CatalogModel throwing = new CatalogModel(
            "broken-model", "broken-model", null, null, false, false, false, false, false, false, null, null, null,
            Status.ACTIVE, new Modalities(List.of(Modality.TEXT), List.of(Modality.TEXT)),
            new Limit(128000, null, null),
            new Cost(new BigDecimal("-1"), BigDecimal.TEN, null, null, null, null, null, List.of()));
        CatalogModel healthy = catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(List.of(throwing, healthy));

        ListAppender<ILoggingEvent> appender = attachAppenderToReconcilerLogger();

        try {
            reconciler.reconcile();

            ArgumentCaptor<AiModel> captor = ArgumentCaptor.forClass(AiModel.class);

            verify(aiModelService).create(captor.capture());

            assertThat(captor.getValue()
                .getName()).isEqualTo("gpt-5.1");

            assertThat(appender.list)
                .as("the broken model's failure must be logged rather than silently dropped")
                .anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getFormattedMessage()
                        .contains("broken-model"));
        } finally {
            detachAppenderFromReconcilerLogger(appender);
        }
    }

    @Test
    void testStillUpdatesExistingRowWhoseCatalogModelHasNoTextOutput() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of(existingRow("dall-e-3", false)));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("dall-e-3", Status.ACTIVE, Modality.IMAGE)));

        reconciler.reconcile();

        verify(aiModelService).updateFromCatalog(any());
    }

    @Test
    void testInsertsModelWithoutCostAsNullRates() {
        CatalogModel unpriced = new CatalogModel(
            "acme-preview", "Acme Preview", null, null, false, false, false, false, false, false, null, null, null,
            Status.ACTIVE, new Modalities(List.of(Modality.TEXT), List.of(Modality.TEXT)),
            new Limit(8192, null, null), null);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of());
        when(modelCatalog.getModels("openai")).thenReturn(List.of(unpriced));

        reconciler.reconcile();

        ArgumentCaptor<AiModel> captor = ArgumentCaptor.forClass(AiModel.class);

        verify(aiModelService).create(captor.capture());

        AiModel created = captor.getValue();

        assertThat(created.getInputCostPerMTokens()).isNull();
        assertThat(created.getOutputCostPerMTokens()).isNull();
        assertThat(created.getContextWindow()).isEqualTo(8192);
    }

    @Test
    void testSecondReconcileAgainstUnchangedCatalogWritesNothing() {
        AiModel alreadyCurrent = existingRow("gpt-5.1", false);

        alreadyCurrent.setCapabilities("temperature,tool_call");
        alreadyCurrent.setContextWindow(400000);
        alreadyCurrent.setInputCostPerMTokens(new BigDecimal("1.25"));
        alreadyCurrent.setOutputCostPerMTokens(BigDecimal.TEN);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(enabledProvider()));
        when(aiModelService.getModelsByProviderId(7L)).thenReturn(List.of(alreadyCurrent));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        reconciler.reconcile();

        verify(aiModelService, never()).updateFromCatalog(any());
    }

    @Test
    void testContinuesToNextProviderWhenOneFails() {
        AiGatewayProvider failing = new AiGatewayProvider("OpenAI", AiGatewayProviderType.OPENAI, "sk-a");
        AiGatewayProvider healthy = new AiGatewayProvider("Anthropic", AiGatewayProviderType.ANTHROPIC, "sk-b");

        setId(failing, 7L);
        setId(healthy, 8L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(failing, healthy));
        when(modelCatalog.getModels("openai")).thenReturn(
            List.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));
        when(aiModelService.getModelsByProviderId(7L)).thenThrow(new IllegalStateException("db down"));
        when(aiModelService.getModelsByProviderId(8L)).thenReturn(List.of());
        when(modelCatalog.getModels("anthropic")).thenReturn(
            List.of(catalogModel("claude-sonnet-4-6", Status.ACTIVE, Modality.TEXT)));

        ListAppender<ILoggingEvent> appender = attachAppenderToReconcilerLogger();

        try {
            reconciler.reconcile();

            verify(aiModelService).create(any());

            assertThat(appender.list)
                .as("the WARN log must name the actual service failure, not a stubbing artifact")
                .anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getThrowableProxy() != null
                    && event.getThrowableProxy()
                        .getClassName()
                        .equals(IllegalStateException.class.getName())
                    && "db down".equals(
                        event.getThrowableProxy()
                            .getMessage()));
        } finally {
            detachAppenderFromReconcilerLogger(appender);
        }
    }

    @Test
    void testCatalogManagedModelIdsReturnsIdWhenCatalogHasEntry() {
        AiModel model = existingRow(7L, "gpt-5.1", 42L, false);

        when(aiGatewayProviderService.getProviders(Set.of(7L))).thenReturn(List.of(enabledProvider()));
        when(modelCatalog.fetchModel("openai", "gpt-5.1"))
            .thenReturn(Optional.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        Set<Long> managedIds = reconciler.catalogManagedModelIds(List.of(model));

        assertThat(managedIds).containsExactly(42L);
    }

    @Test
    void testCatalogManagedModelIdsOmitsIdWhenCatalogHasNoEntry() {
        AiModel model = existingRow(7L, "ft:gpt-4o:acme:x", 42L, false);

        when(aiGatewayProviderService.getProviders(Set.of(7L))).thenReturn(List.of(enabledProvider()));
        when(modelCatalog.fetchModel("openai", "ft:gpt-4o:acme:x")).thenReturn(Optional.empty());

        Set<Long> managedIds = reconciler.catalogManagedModelIds(List.of(model));

        assertThat(managedIds).isEmpty();
    }

    @Test
    void testCatalogManagedModelIdsOmitsIdWhenProviderDisabled() {
        AiModel model = existingRow(7L, "gpt-5.1", 42L, false);

        when(aiGatewayProviderService.getProviders(Set.of(7L))).thenReturn(List.of(disabledProvider()));

        Set<Long> managedIds = reconciler.catalogManagedModelIds(List.of(model));

        assertThat(managedIds).isEmpty();
        verify(modelCatalog, never()).fetchModel(any(), any());
    }

    @Test
    void testCatalogManagedModelIdsIsIndependentOfPinnedFlag() {
        AiModel pinnedModel = existingRow(7L, "gpt-5.1", 42L, true);

        when(aiGatewayProviderService.getProviders(Set.of(7L))).thenReturn(List.of(enabledProvider()));
        when(modelCatalog.fetchModel("openai", "gpt-5.1"))
            .thenReturn(Optional.of(catalogModel("gpt-5.1", Status.ACTIVE, Modality.TEXT)));

        Set<Long> managedIds = reconciler.catalogManagedModelIds(List.of(pinnedModel));

        assertThat(managedIds).containsExactly(42L);
    }

    @Test
    void testCatalogManagedModelIdsBatchesProviderLookupAcrossRows() {
        AiModel first = existingRow(7L, "gpt-5.1", 42L, false);
        AiModel second = existingRow(7L, "gpt-5.1-mini", 43L, false);

        when(aiGatewayProviderService.getProviders(Set.of(7L))).thenReturn(List.of(enabledProvider()));
        when(modelCatalog.fetchModel(any(), any())).thenReturn(Optional.empty());

        reconciler.catalogManagedModelIds(List.of(first, second));

        verify(aiGatewayProviderService, times(1)).getProviders(anyCollection());
    }

    @Test
    void testCatalogManagedModelIdsReturnsEmptySetForEmptyInputWithoutQueryingProviders() {
        Set<Long> managedIds = reconciler.catalogManagedModelIds(List.of());

        assertThat(managedIds).isEmpty();

        verify(aiGatewayProviderService, never()).getProviders(anyCollection());
    }

    private static ListAppender<ILoggingEvent> attachAppenderToReconcilerLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(AiModelCatalogReconcilerImpl.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppenderFromReconcilerLogger(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(AiModelCatalogReconcilerImpl.class);

        logger.detachAppender(appender);
        appender.stop();
    }

    @Nested
    class BundledSnapshot {

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

            verify(aiModelService, org.mockito.Mockito.atLeastOnce())
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

            assertThat(createdByName).containsKeys(
                "gpt-image-1.5", "whisper-large-v3", "whisper-large-v3-turbo", "voxtral-mini-latest",
                "gpt-image-1-mini", "chatgpt-image-latest");

            assertThat(createdByName.get("whisper-large-v3")
                .getContextWindow())
                    .as("a catalog-published context of 0 must normalize to null, not be skipped or thrown")
                    .isNull();
        }
    }
}
