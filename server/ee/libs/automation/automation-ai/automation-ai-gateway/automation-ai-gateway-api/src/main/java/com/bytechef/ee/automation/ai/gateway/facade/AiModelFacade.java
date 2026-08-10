/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.List;

/**
 * Facade for managing AI LLM Gateway models. Hosts the authorization guards so they apply to every caller of the facade
 * rather than only the GraphQL entry point, and keeps them off the shared {@code AiModelService} which the gateway data
 * plane relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiModelFacade {

    AiModel getModel(long id);

    List<AiModel> getModels();

    List<AiModel> getModelsByProviderId(long providerId);

    AiModel create(AiModel model);

    void delete(long id);

    AiModel update(AiModel model);

    /**
     * Runs a models.dev reconcile immediately, rather than waiting for the daily sweep. Used after enabling a provider
     * or after an operator refreshes the bundled snapshot.
     */
    void reconcileCatalog();

    /**
     * Clears the catalog-override flag so the reconciler resumes managing this row.
     */
    AiModel unpin(long id);
}
