/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionProtoMapperTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final ConnectionProtoMapper connectionProtoMapper = new ConnectionProtoMapper(JSON_MAPPER);

    @Test
    void testConnectionRoundTrip() {
        Connection connection = connection();

        assertThat(connectionProtoMapper.toConnection(connectionProtoMapper.toProto(connection)))
            .usingRecursiveComparison()
            .isEqualTo(connection);
    }

    @Test
    void testConnectionDTORoundTrip() {
        ConnectionDTO connectionDTO = new ConnectionDTO(
            true, AuthorizationType.API_KEY, Map.of("token", "abc"), "https://api.airtable.com", "airtable",
            Map.of("key", "secret"), 1, "admin@localhost.com", Instant.parse("2026-10-26T10:00:00Z"),
            Connection.CredentialStatus.VALID, 2, 7L, "user@localhost.com", Instant.parse("2026-10-26T11:00:00Z"),
            "My Airtable", Map.of("key", "secret", "count", 5), ConnectionStatus.ACTIVE, List.of(tag(), tag()), 3);

        assertThat(connectionProtoMapper.toConnectionDTO(connectionProtoMapper.toProto(connectionDTO)))
            .usingRecursiveComparison()
            .isEqualTo(connectionDTO);
    }

    @Test
    void testTagRoundTrip() {
        Tag tag = tag();

        assertThat(connectionProtoMapper.toTag(connectionProtoMapper.toProto(tag)))
            .usingRecursiveComparison()
            .isEqualTo(tag);
    }

    @Test
    void testConnectionWithoutOptionalFieldsRoundTrip() {
        Connection connection = new Connection();

        connection.setComponentName("airtable");
        connection.setName("Bare");

        assertThat(connectionProtoMapper.toConnection(connectionProtoMapper.toProto(connection)))
            .usingRecursiveComparison()
            .isEqualTo(connection);
    }

    private static Connection connection() {
        return JSON_MAPPER.readValue(
            """
                {"authorizationType":"API_KEY","componentName":"airtable","connectionVersion":1,
                "createdBy":"admin@localhost.com","createdDate":"2026-10-26T10:00:00Z","credentialStatus":"INVALID",
                "environmentId":2,"id":7,"lastModifiedBy":"user@localhost.com",
                "lastModifiedDate":"2026-10-26T11:00:00Z","name":"My Airtable",
                "parameters":{"key":"secret","count":5,"nested":{"a":[1,2]}},"status":"PENDING_REASSIGNMENT",
                "tagIds":[1,2],"type":"EMBEDDED","version":3}
                """,
            Connection.class);
    }

    private static Tag tag() {
        return JSON_MAPPER.readValue(
            """
                {"createdBy":"admin@localhost.com","createdDate":"2026-10-26T09:00:00Z","id":4,
                "lastModifiedBy":"admin@localhost.com","lastModifiedDate":"2026-10-26T09:30:00Z","name":"crm",
                "version":2}
                """,
            Tag.class);
    }
}
