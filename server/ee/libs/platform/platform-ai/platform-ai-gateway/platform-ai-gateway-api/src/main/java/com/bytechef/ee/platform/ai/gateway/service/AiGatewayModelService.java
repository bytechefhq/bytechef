/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiGatewayModelService {

    AiGatewayModel create(AiGatewayModel model);

    void delete(long id);

    /**
     * Provider-agnostic lookup by model name (e.g. the {@code gen_ai.response.model} attribute from an OTLP span). If
     * multiple providers ship a model with the same name, returns the one with the lowest id for determinism.
     *
     * @author Ivica Cardic
     * @version ee
     */
    Optional<AiGatewayModel> findByModelIdentifier(String modelIdentifier);

    AiGatewayModel getModel(long id);

    AiGatewayModel getModel(long providerId, String name);

    List<AiGatewayModel> getModels();

    List<AiGatewayModel> getModelsByProviderId(long providerId);

    List<AiGatewayModel> getEnabledModels();

    AiGatewayModel update(AiGatewayModel model);
}
