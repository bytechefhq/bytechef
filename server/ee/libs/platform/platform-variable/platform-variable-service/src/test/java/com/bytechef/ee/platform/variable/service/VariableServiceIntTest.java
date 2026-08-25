/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.bytechef.ee.platform.variable.config.VariableIntTestConfiguration;
import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Round-trips {@link VariableService} through the real {@code property} table (Testcontainers PostgreSQL) instead of a
 * mocked {@code PropertyService} -- the only place the unique constraint, the {@code Scope.EMBEDDED}/null-scopeId
 * shape, and the JDBC value-encryption path are actually exercised.
 *
 * <p>
 * Test methods share one Spring context and one database and never reset state between them, so each method uses a
 * (scope, environmentId) pair no other method writes into -- otherwise assertions become order-dependent on JUnit's
 * unspecified method ordering.
 *
 * @version ee
 */
@SpringBootTest(classes = VariableIntTestConfiguration.class)
@ActiveProfiles("testint")
class VariableServiceIntTest {

    @Autowired
    private VariableService variableService;

    @Test
    void testCreateListUpdateDeleteRoundTrip() {
        Variable created = variableService.create(VariableScope.workspace(1049L), 0L, "API_URL", "https://a");

        assertThat(variableService.getVariables(VariableScope.workspace(1049L), 0L)).extracting(Variable::name)
            .containsExactly("API_URL");
        assertThat(variableService.getVariables(VariableScope.workspace(1049L), 1L)).isEmpty();
        assertThat(variableService.getVariables(VariableScope.embedded(), 0L)).isEmpty();

        variableService.update(VariableScope.workspace(1049L), 0L, created.id(), "BASE_URL", "https://b");

        assertThat(variableService.getVariableMap(VariableScope.workspace(1049L), 0L))
            .containsExactly(Map.entry("BASE_URL", "https://b"));

        // The rename deletes the old row and inserts a new one (see VariableServiceImpl#update), so created.id()
        // no longer identifies a row -- the renamed variable's id must be looked up fresh before deleting it.
        List<Variable> renamedVariables = variableService.getVariables(VariableScope.workspace(1049L), 0L);
        long renamedId = renamedVariables.getFirst()
            .id();

        variableService.delete(VariableScope.workspace(1049L), 0L, renamedId);

        assertThat(variableService.getVariables(VariableScope.workspace(1049L), 0L)).isEmpty();
    }

    @Test
    void testEmbeddedScopeStoresNullScopeId() {
        variableService.create(VariableScope.embedded(), 2L, "REGION", "eu");

        assertThat(variableService.getVariableMap(VariableScope.embedded(), 2L)).containsEntry("REGION", "eu");
        assertThat(variableService.getVariableMap(VariableScope.workspace(1049L), 2L)).doesNotContainKey("REGION");
    }

    @Test
    void testDuplicateNameIsRejected() {
        // Scope.EMBEDDED rows carry a null scope_id, and Postgres unique constraints treat every null as distinct,
        // so uk_property_key_scope_scope_id_environment alone would NOT catch this -- the rejection has to come
        // from VariableServiceImpl's own fetchProperty existence check, which this proves against a real table.
        variableService.create(VariableScope.embedded(), 10L, "DUP", "1");

        assertThatThrownBy(() -> variableService.create(VariableScope.embedded(), 10L, "DUP", "2"))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testRenameOntoExistingNameIsRejected() {
        variableService.create(VariableScope.workspace(1049L), 11L, "FIRST", "1");
        Variable second = variableService.create(VariableScope.workspace(1049L), 11L, "SECOND", "2");

        assertThatThrownBy(
            () -> variableService.update(VariableScope.workspace(1049L), 11L, second.id(), "FIRST", "3"))
                .asInstanceOf(type(ConfigurationException.class))
                .extracting(ConfigurationException::getErrorKey)
                .isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS.getErrorKey());

        // The rejected rename must not have mutated either row.
        assertThat(variableService.getVariableMap(VariableScope.workspace(1049L), 11L))
            .containsExactly(Map.entry("FIRST", "1"), Map.entry("SECOND", "2"));
    }
}
