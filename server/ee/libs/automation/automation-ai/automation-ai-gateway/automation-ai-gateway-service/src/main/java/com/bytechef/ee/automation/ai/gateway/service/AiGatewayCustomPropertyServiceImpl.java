/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayCustomPropertyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayCustomPropertyServiceImpl implements AiGatewayCustomPropertyService {

    private final AiGatewayCustomPropertyRepository aiGatewayCustomPropertyRepository;

    public AiGatewayCustomPropertyServiceImpl(AiGatewayCustomPropertyRepository aiGatewayCustomPropertyRepository) {
        this.aiGatewayCustomPropertyRepository = aiGatewayCustomPropertyRepository;
    }

    @Override
    public AiGatewayCustomProperty create(AiGatewayCustomProperty customProperty) {
        Validate.notNull(customProperty, "'customProperty' must not be null");

        return aiGatewayCustomPropertyRepository.save(customProperty);
    }

    @Override
    public List<AiGatewayCustomProperty> createAll(List<AiGatewayCustomProperty> customProperties) {
        Validate.notNull(customProperties, "'customProperties' must not be null");

        return aiGatewayCustomPropertyRepository.saveAll(customProperties);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayCustomProperty> getCustomPropertiesByTraceId(long traceId) {
        return aiGatewayCustomPropertyRepository.findByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayCustomProperty> getCustomPropertiesByRequestLogId(long requestLogId) {
        return aiGatewayCustomPropertyRepository.findByRequestLogId(requestLogId);
    }
}
