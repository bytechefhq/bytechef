/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiGatewayCustomPropertyServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiGatewayCustomPropertyService aiGatewayCustomPropertyService;

    @Test
    public void testCreateAndFindByTraceId() {
        long traceId = 42L;

        AiGatewayCustomProperty property = new AiGatewayCustomProperty(WORKSPACE_ID, "tier", "gold");

        property.setTraceId(traceId);

        aiGatewayCustomPropertyService.create(property);

        List<AiGatewayCustomProperty> found = aiGatewayCustomPropertyService.getCustomPropertiesByTraceId(traceId);

        assertThat(found)
            .extracting(AiGatewayCustomProperty::getKey)
            .contains("tier");

        assertThat(found)
            .extracting(AiGatewayCustomProperty::getValue)
            .contains("gold");
    }
}
