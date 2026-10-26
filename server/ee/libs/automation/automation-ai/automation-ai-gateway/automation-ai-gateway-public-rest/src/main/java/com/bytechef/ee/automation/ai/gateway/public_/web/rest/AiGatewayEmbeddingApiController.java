/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.EmbeddingDataModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.EmbeddingRequestModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.EmbeddingResponseModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.EmbeddingUsageModel;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayEmbeddingApiController implements EmbeddingApi {

    private final AiGatewayFacade aiGatewayFacade;

    AiGatewayEmbeddingApiController(AiGatewayFacade aiGatewayFacade) {
        this.aiGatewayFacade = aiGatewayFacade;
    }

    @Override
    public ResponseEntity<EmbeddingResponseModel> embeddings(EmbeddingRequestModel embeddingRequestModel) {
        Map<String, String> tags = embeddingRequestModel.getTags() == null
            ? null
            : embeddingRequestModel.getTags()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));

        AiGatewayEmbeddingRequest request = new AiGatewayEmbeddingRequest(
            embeddingRequestModel.getModel(), embeddingRequestModel.getInput(), tags);

        AiGatewayEmbeddingResponse domainResponse = aiGatewayFacade.embedding(request);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        applyGatewayMetadataHeaders(builder, domainResponse.gatewayMetadata());

        return builder.body(toResponseModel(domainResponse));
    }

    private static void applyGatewayMetadataHeaders(
        ResponseEntity.BodyBuilder builder, AiGatewayChatCompletionResponse.GatewayMetadata metadata) {
        if (metadata == null) {
            return;
        }

        if (metadata.provider() != null) {
            builder.header("x-gateway-provider", metadata.provider());
        }

        if (metadata.model() != null) {
            builder.header("x-gateway-model", metadata.model());
        }

        if (metadata.latencyMs() != null) {
            builder.header("x-gateway-latency-ms", String.valueOf(metadata.latencyMs()));
        }

        if (metadata.cacheHit() != null) {
            builder.header("x-gateway-cache-hit", String.valueOf(metadata.cacheHit()));
        }

        if (metadata.routingPolicy() != null) {
            builder.header("x-gateway-routing-policy", metadata.routingPolicy());
        }

        if (metadata.requestId() != null) {
            builder.header("x-gateway-request-id", metadata.requestId());
        }

        if (metadata.budgetWarningRemainingUsd() != null) {
            builder.header(
                "x-gateway-budget-warning",
                metadata.budgetWarningRemainingUsd()
                    .toPlainString());
        }
    }

    private EmbeddingResponseModel toResponseModel(AiGatewayEmbeddingResponse domainResponse) {
        EmbeddingResponseModel responseModel = new EmbeddingResponseModel();

        responseModel.setObject(domainResponse.object());
        responseModel.setModel(domainResponse.model());

        if (domainResponse.data() != null) {
            List<EmbeddingDataModel> embeddingDataModels = domainResponse.data()
                .stream()
                .map(embeddingData -> {
                    EmbeddingDataModel dataModel = new EmbeddingDataModel();

                    dataModel.setObject(embeddingData.object());
                    dataModel.setIndex(embeddingData.index());
                    dataModel.setEmbedding(embeddingData.embedding());

                    return dataModel;
                })
                .toList();

            responseModel.setData(embeddingDataModels);
        }

        if (domainResponse.usage() != null) {
            EmbeddingUsageModel usageModel = new EmbeddingUsageModel();

            usageModel.setPromptTokens(domainResponse.usage()
                .promptTokens());
            usageModel.setTotalTokens(domainResponse.usage()
                .totalTokens());

            responseModel.setUsage(usageModel);
        }

        return responseModel;
    }
}
