/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.ConnectionMapper;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.TagModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Disabled
@WebMvcTest(value = ConnectionApiController.class)
@EmbeddedConfigurationRestConfigurationSharedMocks
public class ConnectionApiControllerIntTest {

    @MockitoBean
    private ConnectionFacade connectionFacade;

    @MockitoBean
    private ConnectionService connectionService;

    @Autowired
    private ConnectionMapper connectionMapper;

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
    public void testDeleteConnection() {
        try {
            this.webTestClient
                .delete()
                .uri("/internal/connections/1")
                .exchange()
                .expectStatus()
                .isOk();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);

        verify(connectionFacade).delete(argument.capture());

        Assertions.assertEquals(1L, argument.getValue());
    }

    @Test
    public void testGetConnection() {
        try {
            ConnectionDTO connectionDTO = getConnection();

            when(connectionFacade.getConnection(1L))
                .thenReturn(connectionDTO);

            this.webTestClient
                .get()
                .uri("/internal/connections/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ConnectionModel.class)
                .isEqualTo(Validate.notNull(connectionMapper.convert(connectionDTO), "connectionModel")
                    .parameters(null));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetConnectionTags() {
        when(connectionFacade.getConnectionTags(ModeType.EMBEDDED))
            .thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        try {
            this.webTestClient
                .get()
                .uri("/internal/connections/tags")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo(1)
                .jsonPath("$.[1].id")
                .isEqualTo(2)
                .jsonPath("$.[0].name")
                .isEqualTo("tag1")
                .jsonPath("$.[1].name")
                .isEqualTo("tag2");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetConnections() {
        ConnectionDTO connectionDTO = getConnection();

        when(connectionFacade.getConnections((String) null, null, List.of(), null, null, ModeType.EMBEDDED))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .contains(Validate.notNull(connectionMapper.convert(connectionDTO), "connectionMapper")
                .parameters(null))
            .hasSize(1);

        when(connectionFacade.getConnections("component1", null, List.of(), null, null, ModeType.EMBEDDED))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections?componentNames=component1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        when(connectionFacade.getConnections(null, 1, List.of(), null, null, ModeType.EMBEDDED))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        when(connectionFacade.getConnections("component1", 1, List.of(), null, null, ModeType.EMBEDDED))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections?componentNames=component1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    public void testPostConnection() {
        ConnectionDTO connectionDTO = getConnection();
        ConnectionModel connectionModel = new ConnectionModel().componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"));

        when(connectionFacade.create(any(), ModeType.EMBEDDED)).thenReturn(getConnection().id());

        try {
            assert connectionDTO.id() != null;
            this.webTestClient
                .post()
                .uri("/internal/connections")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(connectionModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(connectionDTO.id())
                .jsonPath("$.name")
                .isEqualTo(connectionDTO.name())
                .jsonPath("$.parameters")
                .isMap()
                .jsonPath("$.parameters.key1")
                .isEqualTo("value1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<ConnectionDTO> connectionArgumentCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(connectionFacade).create(connectionArgumentCaptor.capture(), ModeType.EMBEDDED);

        assertThat(connectionArgumentCaptor.getValue())
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("parameters", Map.of("key1", "value1"));
    }

    @Test
    public void testPutConnection() {
        ConnectionModel connectionModel = new ConnectionModel().name("name2");

        try {
            this.webTestClient
                .put()
                .uri("/internal/connections/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(connectionModel)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPutConnectionTags() {
        try {
            this.webTestClient
                .put()
                .uri("/project-connections/1/tags")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTagsRequestModel().tags(List.of(new TagModel().name("tag1"))))
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<List<Long>> tagIdsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(connectionService).update(anyLong(), tagIdsArgumentCaptor.capture());

        List<Long> capturedTagIds = tagIdsArgumentCaptor.getValue();

        Iterator<Long> tagIdIterator = capturedTagIds.iterator();

        Long capturedTagId = tagIdIterator.next();

        Assertions.assertEquals(2, capturedTagId);
    }

    private static ConnectionDTO getConnection() {
        return ConnectionDTO.builder()
            .componentName("componentName")
            .id(1L)
            .name("name")
            .parameters(Map.of("key1", "value1"))
            .version(1)
            .build();
    }

    @ComponentScan(basePackages = "com.bytechef.ee.embedded.configuration.web.rest")
    @Configuration
    public static class ConnectionRestTestConfiguration {
    }
}
