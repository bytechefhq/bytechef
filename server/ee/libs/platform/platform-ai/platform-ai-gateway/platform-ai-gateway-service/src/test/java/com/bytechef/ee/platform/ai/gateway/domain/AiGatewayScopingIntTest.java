/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * @version ee
 */
@SpringBootTest(classes = AiGatewayScopingIntTestConfiguration.class)
@TestPropertySource(properties = "spring.liquibase.contexts=configuration")
class AiGatewayScopingIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSecondSettingsRowForSameConnectedUserIsRejectedByUniqueConstraint() {
        insertConnectedUserSettings(300L);

        assertThatThrownBy(() -> insertConnectedUserSettings(300L))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertConnectedUserSettings(long connectedUserId) {
        jdbcTemplate.update(
            "INSERT INTO ai_gateway_connected_user_settings (connected_user_id, created_date, last_modified_date, " +
                "version) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
            new Object[] {
                connectedUserId
            },
            new int[] {
                Types.BIGINT
            });
    }

    @Test
    void testProviderWorkspaceAndConnectedUserBothSetIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> insertProvider("scoping-both-set", AiGatewayProviderType.OPENAI, 1L, 1L))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testSecondProviderForSameConnectedUserAndTypeIsRejectedByPartialUniqueIndex() {
        insertProvider("scoping-connected-user-openai-first", AiGatewayProviderType.OPENAI, null, 100L);

        assertThatThrownBy(
            () -> insertProvider("scoping-connected-user-openai-second", AiGatewayProviderType.OPENAI, null, 100L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testTwoProvidersForSameConnectedUserButDifferentTypesBothInsert() {
        assertThatCode(() -> {
            insertProvider("scoping-connected-user-openai", AiGatewayProviderType.OPENAI, null, 101L);
            insertProvider("scoping-connected-user-anthropic", AiGatewayProviderType.ANTHROPIC, null, 101L);
        }).doesNotThrowAnyException();
    }

    @Test
    void testTwoProvidersForDifferentConnectedUsersSameTypeBothInsert() {
        assertThatCode(() -> {
            insertProvider("scoping-connected-user-a-openai", AiGatewayProviderType.OPENAI, null, 200L);
            insertProvider("scoping-connected-user-b-openai", AiGatewayProviderType.OPENAI, null, 201L);
        }).doesNotThrowAnyException();
    }

    @Test
    void testProviderRowWithBothScopeColumnsNullStillInserts() {
        assertThatCode(() -> {
            insertProvider("scoping-default-tier-openai-a", AiGatewayProviderType.OPENAI, null, null);
            insertProvider("scoping-default-tier-openai-b", AiGatewayProviderType.OPENAI, null, null);
        }).doesNotThrowAnyException();
    }

    private void insertProvider(String name, AiGatewayProviderType type, Long workspaceId, Long connectedUserId) {
        jdbcTemplate.update(
            "INSERT INTO ai_gateway_provider (name, type, api_key, workspace_id, connected_user_id, enabled, " +
                "created_by, created_date, last_modified_by, last_modified_date, version) " +
                "VALUES (?, ?, 'sk-test', ?, ?, true, 'test', now(), 'test', now(), 0)",
            new Object[] {
                name, type.ordinal(), workspaceId, connectedUserId
            },
            new int[] {
                Types.VARCHAR, Types.INTEGER, Types.BIGINT, Types.BIGINT
            });
    }
}
