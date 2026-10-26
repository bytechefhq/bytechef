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

    void reconcileCatalog();

    AiModel unpin(long id);
}
