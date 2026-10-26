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
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiModelService {

    AiModel create(AiModel model);

    void delete(long id);

    Optional<AiModel> findByModelIdentifier(String modelIdentifier);

    AiModel getModel(long id);

    AiModel getModel(long providerId, String name);

    List<AiModel> getModels();

    List<AiModel> getModelsByProviderId(long providerId);

    List<AiModel> getEnabledModels();

    AiModel update(AiModel model);

    AiModel updateFromCatalog(AiModel model);

    AiModel unpin(long id);
}
