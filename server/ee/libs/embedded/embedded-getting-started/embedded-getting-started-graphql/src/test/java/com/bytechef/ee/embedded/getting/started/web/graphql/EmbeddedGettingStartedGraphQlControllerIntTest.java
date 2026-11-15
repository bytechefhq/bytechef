/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.getting.started.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.getting.started.dto.EmbeddedGettingStartedDTO;
import com.bytechef.ee.embedded.getting.started.dto.LastFailedCallDTO;
import com.bytechef.ee.embedded.getting.started.facade.EmbeddedGettingStartedFacade;
import com.bytechef.ee.embedded.getting.started.web.graphql.config.EmbeddedGettingStartedGraphQlTestConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
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
    EmbeddedGettingStartedGraphQlController.class, EmbeddedGettingStartedGraphQlTestConfiguration.class
})
@GraphQlTest(
    controllers = EmbeddedGettingStartedGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedGettingStartedGraphQlControllerIntTest {

    private static final String API_BASE_URL_QUERY = """
        query {
            embeddedGettingStarted {
                apiBaseUrl
            }
        }
        """;
    private static final Instant CREATED_DATE = Instant.parse("2026-11-11T10:15:30Z");

    @MockitoBean
    private ApplicationProperties applicationProperties;

    @MockitoBean
    private EmbeddedGettingStartedFacade embeddedGettingStartedFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testEmbeddedGettingStartedMapsEveryField() {
        when(embeddedGettingStartedFacade.getEmbeddedGettingStarted())
            .thenReturn(new EmbeddedGettingStartedDTO(
                true, true, true, true, true, false, true, true, "user-1", true,
                new LastFailedCallDTO("FAILED", "boom", CREATED_DATE), false, 7L, 8L, 9L,
                "httpClient", 1));

        this.graphQlTester
            .document("""
                query {
                    embeddedGettingStarted {
                        apiKeyCreated
                        completed
                        componentName
                        componentVersion
                        firstCallMade
                        integrationConfigurationCreated
                        integrationConfigurationEnabled
                        integrationCreated
                        integrationHasWorkflows
                        integrationId
                        integrationInstanceConfigurationId
                        integrationPublished
                        lastFailedCall {
                            createdDate
                            errorMessage
                            status
                        }
                        testUserAdded
                        testUserConnected
                        testUserExternalId
                        testUserId
                    }
                }
                """)
            .execute()
            .path("embeddedGettingStarted.apiKeyCreated")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.completed")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("embeddedGettingStarted.componentName")
            .entity(String.class)
            .isEqualTo("httpClient")
            .path("embeddedGettingStarted.componentVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("embeddedGettingStarted.firstCallMade")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.integrationConfigurationCreated")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.integrationHasWorkflows")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.integrationConfigurationEnabled")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("embeddedGettingStarted.integrationCreated")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.integrationId")
            .entity(Long.class)
            .isEqualTo(7L)
            .path("embeddedGettingStarted.integrationInstanceConfigurationId")
            .entity(Long.class)
            .isEqualTo(8L)
            .path("embeddedGettingStarted.integrationPublished")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.lastFailedCall.createdDate")
            .entity(Long.class)
            .isEqualTo(CREATED_DATE.toEpochMilli())
            .path("embeddedGettingStarted.lastFailedCall.errorMessage")
            .entity(String.class)
            .isEqualTo("boom")
            .path("embeddedGettingStarted.lastFailedCall.status")
            .entity(String.class)
            .isEqualTo("FAILED")
            .path("embeddedGettingStarted.testUserAdded")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.testUserConnected")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("embeddedGettingStarted.testUserExternalId")
            .entity(String.class)
            .isEqualTo("user-1")
            .path("embeddedGettingStarted.testUserId")
            .entity(Long.class)
            .isEqualTo(9L);
    }

    @Test
    void testEmbeddedGettingStartedWithoutLastFailedCallReturnsNull() {
        when(embeddedGettingStartedFacade.getEmbeddedGettingStarted())
            .thenReturn(
                new EmbeddedGettingStartedDTO(
                    false, false, false, false, false, false, false, false, null, false, null, false, null, null, null,
                    null, null));

        this.graphQlTester
            .document("""
                query {
                    embeddedGettingStarted {
                        lastFailedCall {
                            status
                        }
                        testUserExternalId
                    }
                }
                """)
            .execute()
            .path("embeddedGettingStarted.lastFailedCall")
            .valueIsNull()
            .path("embeddedGettingStarted.testUserExternalId")
            .valueIsNull();
    }

    @Test
    void testEmbeddedGettingStartedBuildsTheApiBaseUrlFromThePublicUrl() {
        mockEmptyEmbeddedGettingStarted();

        when(applicationProperties.getPublicUrl()).thenReturn("https://app.example.com/");

        this.graphQlTester
            .document(API_BASE_URL_QUERY)
            .execute()
            .path("embeddedGettingStarted.apiBaseUrl")
            .entity(String.class)
            .isEqualTo("https://app.example.com/api/embedded/v1");
    }

    @Test
    void testEmbeddedGettingStartedReturnsNullApiBaseUrlWithoutPublicUrl() {
        mockEmptyEmbeddedGettingStarted();

        when(applicationProperties.getPublicUrl()).thenReturn(null);

        this.graphQlTester
            .document(API_BASE_URL_QUERY)
            .execute()
            .path("embeddedGettingStarted.apiBaseUrl")
            .valueIsNull();
    }

    @Test
    void testEmbeddedGettingStartedReturnsNullApiBaseUrlWithBlankPublicUrl() {
        mockEmptyEmbeddedGettingStarted();

        when(applicationProperties.getPublicUrl()).thenReturn("  ");

        this.graphQlTester
            .document(API_BASE_URL_QUERY)
            .execute()
            .path("embeddedGettingStarted.apiBaseUrl")
            .valueIsNull();
    }

    @Test
    void testEmbeddedGettingStartedCompletedReturnsFacadeValue() {
        when(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).thenReturn(true);

        this.graphQlTester
            .document("""
                query {
                    embeddedGettingStartedCompleted
                }
                """)
            .execute()
            .path("embeddedGettingStartedCompleted")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    private void mockEmptyEmbeddedGettingStarted() {
        when(embeddedGettingStartedFacade.getEmbeddedGettingStarted())
            .thenReturn(
                new EmbeddedGettingStartedDTO(
                    false, false, false, false, false, false, false, false, null, false, null, false, null, null, null,
                    null, null));
    }
}
