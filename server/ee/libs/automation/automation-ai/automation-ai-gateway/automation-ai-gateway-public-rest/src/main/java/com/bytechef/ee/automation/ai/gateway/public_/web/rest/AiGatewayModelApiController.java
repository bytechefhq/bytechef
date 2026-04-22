/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ModelListModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ModelModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayModelApiController implements ModelApi {

    private final AiGatewayModelService aiGatewayModelService;
    private final AiGatewayProviderService aiGatewayProviderService;

    AiGatewayModelApiController(
        AiGatewayModelService aiGatewayModelService,
        AiGatewayProviderService aiGatewayProviderService) {

        this.aiGatewayModelService = aiGatewayModelService;
        this.aiGatewayProviderService = aiGatewayProviderService;
    }

    @Override
    public ResponseEntity<ModelListModel> listModels() {
        List<AiGatewayModel> models = aiGatewayModelService.getEnabledModels();

        Map<Long, AiGatewayProvider> providerMap = aiGatewayProviderService.getEnabledProviders()
            .stream()
            .collect(Collectors.toMap(AiGatewayProvider::getId, Function.identity()));

        List<ModelModel> modelModels = models.stream()
            .filter(model -> providerMap.containsKey(model.getProviderId()))
            .map(model -> {
                AiGatewayProvider provider = providerMap.get(model.getProviderId());
                String modelId = provider.getType()
                    .name()
                    .toLowerCase() + "/" + model.getName();

                ModelModel modelModel = new ModelModel();

                modelModel.setId(modelId);
                modelModel.setObject("model");
                modelModel.setOwnedBy(provider.getType()
                    .name()
                    .toLowerCase());

                return modelModel;
            })
            .toList();

        ModelListModel modelListModel = new ModelListModel();

        modelListModel.setObject("list");
        modelListModel.setData(modelModels);

        return ResponseEntity.ok(modelListModel);
    }

    @Override
    public ResponseEntity<ModelModel> getModel(String modelId) {
        String[] parts = modelId.split("/", 2);

        if (parts.length != 2) {
            return ResponseEntity.badRequest()
                .build();
        }

        String providerTypeName = parts[0];
        String modelName = parts[1];

        AiGatewayProvider matchingProvider = aiGatewayProviderService.getEnabledProviders()
            .stream()
            .filter(provider -> provider.getType()
                .name()
                .equalsIgnoreCase(providerTypeName))
            .findFirst()
            .orElse(null);

        if (matchingProvider == null) {
            return ResponseEntity.notFound()
                .build();
        }

        AiGatewayModel matchingModel = aiGatewayModelService.getModelsByProviderId(matchingProvider.getId())
            .stream()
            .filter(model -> model.getName()
                .equals(modelName) && model.isEnabled())
            .findFirst()
            .orElse(null);

        if (matchingModel == null) {
            return ResponseEntity.notFound()
                .build();
        }

        ModelModel modelModel = new ModelModel();

        modelModel.setId(modelId);
        modelModel.setObject("model");
        modelModel.setOwnedBy(providerTypeName);

        return ResponseEntity.ok(modelModel);
    }
}
