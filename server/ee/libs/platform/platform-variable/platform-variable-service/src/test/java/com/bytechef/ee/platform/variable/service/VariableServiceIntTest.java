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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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
        // so uk_property_key_scope_scope_id_environment alone does NOT catch this. Sequentially, the rejection
        // comes from VariableServiceImpl's own fetchProperty existence check; the partial unique index
        // uk_property_key_scope_environment_null_scope_id (20260825000001) backs it up for the concurrent case --
        // see testConcurrentEmbeddedCreateOfSameNameRejectsOneSide below, which proves that constraint fires.
        variableService.create(VariableScope.embedded(), 10L, "DUP", "1");

        assertThatThrownBy(() -> variableService.create(VariableScope.embedded(), 10L, "DUP", "2"))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testConcurrentEmbeddedCreateOfSameNameRejectsOneSide() throws Exception {
        // Reproduces the original bug directly: two threads both call create() for the same embedded variable
        // name at the same time, so both can pass VariableServiceImpl's pre-save fetchProperty existence check
        // before either has committed. Before this change, both inserts succeeded (Scope.EMBEDDED's scope_id is
        // always null, and uk_property_key_scope_scope_id_environment never fires when scope_id is null on both
        // sides), leaving two rows for the same key -- every later read/write for that key then failed with
        // IncorrectResultSizeDataAccessException. With uk_property_key_scope_environment_null_scope_id in place,
        // exactly one insert wins and the other is translated to VARIABLE_NAME_ALREADY_EXISTS.
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Callable<Optional<ConfigurationException>> raceTask = () -> {
                start.await();

                try {
                    variableService.create(VariableScope.embedded(), 21L, "RACE", "value");

                    return Optional.empty();
                } catch (ConfigurationException configurationException) {
                    return Optional.of(configurationException);
                }
            };

            Future<Optional<ConfigurationException>> firstFuture = executorService.submit(raceTask);
            Future<Optional<ConfigurationException>> secondFuture = executorService.submit(raceTask);

            start.countDown();

            Optional<ConfigurationException> firstOutcome = firstFuture.get(10, TimeUnit.SECONDS);
            Optional<ConfigurationException> secondOutcome = secondFuture.get(10, TimeUnit.SECONDS);

            List<ConfigurationException> rejections = Stream.of(firstOutcome, secondOutcome)
                .flatMap(Optional::stream)
                .toList();

            assertThat(rejections)
                .as("exactly one racer must be rejected as a duplicate")
                .hasSize(1);
            assertThat(rejections.getFirst()
                .getErrorKey()).isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS.getErrorKey());

            assertThat(variableService.getVariables(VariableScope.embedded(), 21L)).extracting(Variable::name)
                .containsExactly("RACE");
        } finally {
            executorService.shutdownNow();
        }
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
