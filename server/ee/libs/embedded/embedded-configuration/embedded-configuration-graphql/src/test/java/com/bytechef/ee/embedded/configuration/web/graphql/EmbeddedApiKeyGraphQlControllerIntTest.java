/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    EmbeddedApiKeyGraphQlController.class
})
@GraphQlTest(
    controllers = EmbeddedApiKeyGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedApiKeyGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private EmbeddedApiKeyFacade embeddedApiKeyFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Test
    void testCreateEmbeddedApiKeyReturnsTheSecretAndReusesThePlatformApiKeyType() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.PRODUCTION);
        when(embeddedApiKeyFacade.createApiKey(any(ApiKey.class))).thenReturn("the-secret");

        this.graphQlTester
            .document("""
                mutation {
                    createEmbeddedApiKey(input: {name: "my key", environmentId: "1"})
                }
                """)
            .execute()
            .path("createEmbeddedApiKey")
            .entity(String.class)
            .isEqualTo("the-secret");
    }

    @Test
    void testEmbeddedApiKeysListsTheKeysOfTheEnvironment() {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(5L);
        apiKey.setName("shared key");

        when(embeddedApiKeyFacade.getApiKeys(1L)).thenReturn(List.of(apiKey));

        this.graphQlTester
            .document("""
                query {
                    embeddedApiKeys(environmentId: "1") {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("embeddedApiKeys[0].name")
            .entity(String.class)
            .isEqualTo("shared key");

        verify(embeddedApiKeyFacade).getApiKeys(1L);
    }
}
