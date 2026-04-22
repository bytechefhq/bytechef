/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.TagListModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.TagModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayTagService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
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
class AiGatewayRoutingPolicyTagApiController implements TagApi {

    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;
    private final AiGatewayTagService aiGatewayTagService;

    AiGatewayRoutingPolicyTagApiController(
        AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService,
        AiGatewayTagService aiGatewayTagService) {

        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
        this.aiGatewayTagService = aiGatewayTagService;
    }

    @Override
    public ResponseEntity<TagListModel> listTags(Long routingPolicyId) {
        AiGatewayRoutingPolicy routingPolicy = aiGatewayRoutingPolicyService.getRoutingPolicy(routingPolicyId);

        List<TagModel> tagModels = routingPolicy.getTagIds()
            .stream()
            .map(aiGatewayTagService::getTag)
            .map(AiGatewayRoutingPolicyTagApiController::toTagModel)
            .toList();

        TagListModel listModel = new TagListModel();

        listModel.setObject("list");
        listModel.setData(tagModels);

        return ResponseEntity.ok(listModel);
    }

    private static TagModel toTagModel(AiGatewayTag tag) {
        TagModel tagModel = new TagModel();

        tagModel.setKey(tag.getName());

        return tagModel;
    }
}
