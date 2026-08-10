/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiModelService {

    AiModel create(AiModel model);

    void delete(long id);

    /**
     * Provider-agnostic lookup by model name (e.g. the {@code gen_ai.response.model} attribute from an OTLP span). If
     * multiple providers ship a model with the same name, returns the one with the lowest id for determinism.
     *
     * @author Ivica Cardic
     * @version ee
     */
    Optional<AiModel> findByModelIdentifier(String modelIdentifier);

    AiModel getModel(long id);

    AiModel getModel(long providerId, String name);

    List<AiModel> getModels();

    List<AiModel> getModelsByProviderId(long providerId);

    List<AiModel> getEnabledModels();

    AiModel update(AiModel model);

    /**
     * Applies catalog-sourced values without pinning the row. The reconciler must not route through
     * {@link #update(AiModel)} — that method treats any change to a catalog-owned field as an administrator override,
     * so a reconcile would pin every row it touched and immediately stop managing it.
     */
    AiModel updateFromCatalog(AiModel model);

    /**
     * Clears the catalog-override flag, handing the row back to the reconciler. The next reconcile overwrites its
     * catalog-owned fields.
     *
     * @return the saved model, so callers do not need a second read-only transaction just to hand back the current
     *         state.
     */
    AiModel unpin(long id);
}
