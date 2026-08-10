/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.model.catalog.repository.AiModelRepository;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the {@link AiModelDeleteListener} seam: dependent modules (the AI Gateway's model deployments) cascade through
 * contributed listeners, which must run before the row delete so their cleanup sees the row's id still referencing a
 * live aggregate.
 *
 * @version ee
 */
class AiModelServiceDeleteTest {

    @Test
    void testDeleteInvokesListenersBeforeRowDelete() {
        AiModelRepository aiModelRepository = mock(AiModelRepository.class);
        AiModelDeleteListener aiModelDeleteListener = mock(AiModelDeleteListener.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider = mock(ObjectProvider.class);

        when(aiModelDeleteListenerProvider.orderedStream()).thenReturn(Stream.of(aiModelDeleteListener));

        AiModelServiceImpl aiModelService = new AiModelServiceImpl(aiModelDeleteListenerProvider, aiModelRepository);

        aiModelService.delete(5L);

        InOrder inOrder = inOrder(aiModelDeleteListener, aiModelRepository);

        inOrder.verify(aiModelDeleteListener)
            .beforeDelete(5L);
        inOrder.verify(aiModelRepository)
            .deleteById(5L);
    }

    @Test
    void testDeleteWithoutListenersStillRemovesRow() {
        AiModelRepository aiModelRepository = mock(AiModelRepository.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider = mock(ObjectProvider.class);

        when(aiModelDeleteListenerProvider.orderedStream()).thenReturn(Stream.empty());

        AiModelServiceImpl aiModelService = new AiModelServiceImpl(aiModelDeleteListenerProvider, aiModelRepository);

        aiModelService.delete(7L);

        verify(aiModelRepository).deleteById(7L);
    }
}
