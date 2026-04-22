/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayModelService {

    AiGatewayModel create(AiGatewayModel model);

    void delete(long id);

    AiGatewayModel getModel(long id);

    AiGatewayModel getModel(long providerId, String name);

    List<AiGatewayModel> getModels();

    List<AiGatewayModel> getModelsByProviderId(long providerId);

    List<AiGatewayModel> getEnabledModels();

    AiGatewayModel update(AiGatewayModel model);
}
