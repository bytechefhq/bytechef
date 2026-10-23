/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO.RejectedRow;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.ee.embedded.connected.user.web.rest.config.ConnectedUserRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ConnectedUserRestTestConfiguration.class)
@WebMvcTest(ConnectedUserApiController.class)
class ConnectedUserApiControllerIntTest {

    @MockitoBean
    private ConnectedUserFacade connectedUserFacade;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    void testCreateConnectedUser() {
        when(connectedUserFacade.createConnectedUser(
            eq("user-1"), eq(Environment.STAGING), eq("Jane"), eq("jane@example.com"), eq(Map.of("plan", "pro"))))
                .thenReturn(connectedUserDTO(5L, 0));

        this.webTestClient
            .post()
            .uri("/internal/connected-users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"externalId\":\"user-1\",\"name\":\"Jane\",\"email\":\"jane@example.com\"," +
                    "\"metadata\":{\"plan\":\"pro\"},\"environmentId\":1}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(5)
            .jsonPath("$.externalId")
            .isEqualTo("user-1");

        verify(connectedUserFacade).createConnectedUser(
            "user-1", Environment.STAGING, "Jane", "jane@example.com", Map.of("plan", "pro"));
    }

    @Test
    void testCreateConnectedUserWithoutOptionalFieldsPassesNullsAndEmptyMetadata() {
        when(connectedUserFacade.createConnectedUser(any(), any(), any(), any(), any()))
            .thenReturn(connectedUserDTO(6L, 0));

        this.webTestClient
            .post()
            .uri("/internal/connected-users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"externalId\":\"user-2\",\"environmentId\":0}")
            .exchange()
            .expectStatus()
            .isOk();

        verify(connectedUserFacade).createConnectedUser(
            eq("user-2"), eq(Environment.DEVELOPMENT), isNull(), isNull(), eq(Map.of()));
    }

    @Test
    void testImportConnectedUsers() {
        when(connectedUserFacade.importConnectedUsers(any(Environment.class), any(InputStream.class)))
            .thenReturn(
                new ConnectedUserImportResultDTO(2, List.of("dup"), List.of(new RejectedRow(4, "Row has 1 cell"))));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder
            .part("file", new ByteArrayResource("externalId\na\nb\n".getBytes()) {

                @Override
                public String getFilename() {
                    return "users.csv";
                }
            })
            .contentType(MediaType.parseMediaType("text/csv"));

        this.webTestClient
            .post()
            .uri("/internal/connected-users/import?environmentId=2")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.createdCount")
            .isEqualTo(2)
            .jsonPath("$.skippedExternalIds[0]")
            .isEqualTo("dup")
            .jsonPath("$.rejectedRows[0].rowNumber")
            .isEqualTo(4)
            .jsonPath("$.rejectedRows[0].reason")
            .isEqualTo("Row has 1 cell");

        verify(connectedUserFacade).importConnectedUsers(eq(Environment.PRODUCTION), any(InputStream.class));
    }

    @Test
    void testUpdateConnectedUser() {
        when(connectedUserFacade.updateConnectedUser(anyLong(), any(), any(), any(), anyInt()))
            .thenReturn(connectedUserDTO(7L, 4));

        this.webTestClient
            .put()
            .uri("/internal/connected-users/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"Jane\",\"email\":null,\"metadata\":{\"plan\":\"free\"},\"version\":3}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(7);

        ArgumentCaptor<Integer> versionCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(connectedUserFacade).updateConnectedUser(
            eq(7L), eq("Jane"), isNull(), eq(Map.of("plan", "free")), versionCaptor.capture());

        org.junit.jupiter.api.Assertions.assertEquals(3, versionCaptor.getValue());
    }

    @Test
    void testUpdateConnectedUserWithStaleVersionReturnsConflict() {
        when(connectedUserFacade.updateConnectedUser(anyLong(), any(), any(), any(), anyInt()))
            .thenThrow(new OptimisticLockingFailureException("stale"));

        this.webTestClient
            .put()
            .uri("/internal/connected-users/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"version\":1}")
            .exchange()
            .expectStatus()
            .isEqualTo(409);
    }

    @Test
    void testCreateConnectedUserWithInvalidInputReturnsBadRequest() {
        when(connectedUserFacade.createConnectedUser(any(), any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("External ID 'user-1' already exists"));

        this.webTestClient
            .post()
            .uri("/internal/connected-users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"externalId\":\"user-1\",\"environmentId\":1}")
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    void testCreateConnectedUserWithUnknownEnvironmentIdReturnsBadRequest() {
        this.webTestClient
            .post()
            .uri("/internal/connected-users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"externalId\":\"user-1\",\"environmentId\":7}")
            .exchange()
            .expectStatus()
            .isBadRequest();

        verify(connectedUserFacade, never()).createConnectedUser(any(), any(), any(), any(), any());
    }

    @Test
    void testCreateConnectedUserWithoutEnvironmentIdReturnsBadRequest() {
        this.webTestClient
            .post()
            .uri("/internal/connected-users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"externalId\":\"user-1\"}")
            .exchange()
            .expectStatus()
            .isBadRequest();

        verify(connectedUserFacade, never()).createConnectedUser(any(), any(), any(), any(), any());
    }

    @Test
    void testImportConnectedUsersWithUnknownEnvironmentIdReturnsBadRequest() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder
            .part("file", new ByteArrayResource("externalId\na\n".getBytes()) {

                @Override
                public String getFilename() {
                    return "users.csv";
                }
            })
            .contentType(MediaType.parseMediaType("text/csv"));

        this.webTestClient
            .post()
            .uri("/internal/connected-users/import?environmentId=-1")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus()
            .isBadRequest();

        verify(connectedUserFacade, never()).importConnectedUsers(any(Environment.class), any(InputStream.class));
    }

    private static ConnectedUserDTO connectedUserDTO(long id, int version) {
        return new ConnectedUserDTO(
            null, Instant.now(), null, true, Environment.STAGING, id == 5L ? "user-1" : "user-x", id, List.of(),
            Map.of(), null, Instant.now(), null, version);
    }
}
