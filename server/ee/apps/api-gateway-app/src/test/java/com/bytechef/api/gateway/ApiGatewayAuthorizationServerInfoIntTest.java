/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true",
        "bytechef.oauth2.resource-server.issuers[0].self=true",
        "bytechef.oauth2.resource-server.issuers[0].tenant-claim=tenant_id",
        "bytechef.oauth2.resource-server.issuers[0].uri=http://localhost:8080"
    })
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
class ApiGatewayAuthorizationServerInfoIntTest {

    @Autowired
    private InfoEndpoint infoEndpoint;

    @Test
    void testInfoReportsTheAuthorizationServerWhenEnabledWithASelfIssuer() {
        assertThat(infoEndpoint.info())
            .extractingByKey("oauth2")
            .isEqualTo(Map.of("authorizationServer", Map.of("enabled", "true")));
    }
}
