/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.repository.AiModelRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiModelPinningTest {

    @Mock
    private ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider;

    @Mock
    private AiModelRepository aiModelRepository;

    private AiModelServiceImpl aiModelService;

    private static AiModel existingModel() {
        AiModel model = new AiModel(1L, "gpt-5.1");

        setIdViaReflection(model, 1L);

        model.setAlias("fast");
        model.setCapabilities("reasoning,tool_call");
        model.setContextWindow(400000);
        model.setInputCostPerMTokens(new BigDecimal("1.25"));
        model.setOutputCostPerMTokens(new BigDecimal("10.00"));

        return model;
    }

    // AiModel exposes no setId(...): production id assignment goes through Spring Data reflection on the
    // private no-arg constructor, so a copy standing in for a GraphQL round-trip payload needs the same reflection
    // trick to carry an id that update(...) can look up.
    private static AiModel incomingCopyOf(AiModel source) {
        AiModel model = new AiModel(source.getProviderId(), source.getName());

        setIdViaReflection(model, source.getId());

        model.setAlias(source.getAlias());
        model.setCapabilities(source.getCapabilities());
        model.setContextWindow(source.getContextWindow());
        model.setEnabled(source.isEnabled());
        model.setInputCostPerMTokens(source.getInputCostPerMTokens());
        model.setOutputCostPerMTokens(source.getOutputCostPerMTokens());

        return model;
    }

    private static void setIdViaReflection(AiModel target, long id) {
        try {
            Field idField = target.getClass()
                .getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }
    }

    @BeforeEach
    void setUp() {
        when(aiModelDeleteListenerProvider.orderedStream()).thenReturn(Stream.empty());

        aiModelService = new AiModelServiceImpl(aiModelDeleteListenerProvider, aiModelRepository);
    }

    @Test
    void testUpdatePinsRowWhenCostChanges() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setInputCostPerMTokens(new BigDecimal("0.99"));

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isTrue();
    }

    @Test
    void testUpdatePinsRowWhenOutputCostChanges() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setOutputCostPerMTokens(new BigDecimal("8.00"));

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isTrue();
    }

    @Test
    void testUpdatePinsRowWhenContextWindowChanges() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setContextWindow(200000);

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isTrue();
    }

    @Test
    void testUpdatePinsRowWhenCapabilitiesChange() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setCapabilities("reasoning");

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isTrue();
    }

    @Test
    void testUpdateDoesNotPinWhenOnlyAliasChanges() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setAlias("renamed");

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isFalse();
    }

    @Test
    void testUpdateDoesNotPinWhenClientRoundTripsIdenticalValues() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel updated = aiModelService.update(incomingCopyOf(existing));

        assertThat(updated.isCatalogPinned()).isFalse();
    }

    // BigDecimal.equals() distinguishes 1.25 from 1.250; a client that re-serializes the same rate at a different
    // scale must not trip the pin. This must go through decimalChanged's compareTo, not equals — asserting on a
    // freshly-constructed BigDecimal (rather than reusing existing.getInputCostPerMTokens() by reference) is what
    // makes the test actually exercise that comparison instead of trivially passing on reference/value equality.
    @Test
    void testUpdateDoesNotPinWhenInputCostRescaledToEquivalentValue() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setInputCostPerMTokens(new BigDecimal("1.250"));

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isFalse();
    }

    @Test
    void testUpdateDoesNotPinWhenOutputCostRescaledToEquivalentValue() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setOutputCostPerMTokens(new BigDecimal("10.000"));

        AiModel updated = aiModelService.update(incoming);

        assertThat(updated.isCatalogPinned()).isFalse();
    }

    @Test
    void testUpdateFromCatalogNeverPins() {
        AiModel existing = existingModel();

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel incoming = incomingCopyOf(existing);

        incoming.setInputCostPerMTokens(new BigDecimal("2.50"));

        AiModel updated = aiModelService.updateFromCatalog(incoming);

        assertThat(updated.isCatalogPinned()).isFalse();
        assertThat(updated.getInputCostPerMTokens()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    void testUnpinClearsTheFlag() {
        AiModel existing = existingModel();

        existing.setCatalogPinned(true);

        when(aiModelRepository.findById(any())).thenReturn(Optional.of(existing));
        when(aiModelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiModel unpinned = aiModelService.unpin(1L);

        assertThat(existing.isCatalogPinned()).isFalse();
        assertThat(unpinned.isCatalogPinned()).isFalse();
    }
}
