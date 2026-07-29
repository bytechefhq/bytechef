/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProto;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionProtoMapperTest {

    @Test
    void testRoundTripPreservesScalarAndEnumFields() {
        Connection connection = buildConnection();

        Connection result = ConnectionProtoMapper.toDomain(ConnectionProtoMapper.toProto(connection));

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getName()).isEqualTo("Prod Slack");
        assertThat(result.getComponentName()).isEqualTo("slack");
        assertThat(result.getConnectionVersion()).isEqualTo(3);
        assertThat(result.getEnvironmentId()).isEqualTo(1);
        assertThat(result.getVersion()).isEqualTo(7);
        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.REVOKED);
        assertThat(result.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(result.getType()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(result.getTagIds()).containsExactly(1L, 2L);
    }

    @Test
    void testRoundTripPreservesDynamicParametersViaStruct() {
        Connection connection = buildConnection();

        Connection result = ConnectionProtoMapper.toDomain(ConnectionProtoMapper.toProto(connection));

        Map<String, ?> parameters = result.getParameters();

        assertThat(parameters.get("token")).isEqualTo("xoxb-123");
        assertThat(parameters.get("enabled")).isEqualTo(true);
        assertThat(parameters.get("scopes")).isEqualTo(List.of("chat:write", "channels:read"));
        assertThat(parameters.get("nested")).isEqualTo(Map.of("teamId", "T42"));

        // Documented Struct limitation: numbers travel as double; the mapper narrows integral doubles
        // back to Long, so an integer parameter survives value-wise as 30L (not the original Integer 30).
        assertThat(parameters.get("retries")).isEqualTo(30L);
    }

    @Test
    void testGetConnectionRequestEnumMapping() {
        ConnectionProto proto = ConnectionProtoMapper.toProto(buildConnection());

        assertThat(proto.getStatus()
            .name()).isEqualTo(ConnectionStatus.REVOKED.name());
        assertThat(proto.getType()
            .name()).isEqualTo(PlatformType.EMBEDDED.name());
    }

    private static Connection buildConnection() {
        Connection connection = new Connection();

        connection.setId(42L);
        connection.setName("Prod Slack");
        connection.setComponentName("slack");
        connection.setConnectionVersion(3);
        connection.setEnvironmentId(1);
        connection.setVersion(7);
        connection.setStatus(ConnectionStatus.REVOKED);
        connection.setVisibility(ResourceVisibility.WORKSPACE);
        connection.setType(PlatformType.EMBEDDED);
        connection.setTagIds(List.of(1L, 2L));
        connection.setParameters(
            Map.of(
                "token", "xoxb-123",
                "enabled", true,
                "retries", 30,
                "scopes", List.of("chat:write", "channels:read"),
                "nested", Map.of("teamId", "T42")));

        return connection;
    }
}
