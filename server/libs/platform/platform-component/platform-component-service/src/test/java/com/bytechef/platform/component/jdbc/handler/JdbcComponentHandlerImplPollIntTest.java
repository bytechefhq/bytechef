/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.jdbc.handler;

import static com.bytechef.platform.component.definition.JdbcComponentDsl.jdbcComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.operation.QueryJdbcOperation;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
public class JdbcComponentHandlerImplPollIntTest {

    private static final int BATCH_SIZE = QueryJdbcOperation.DEFAULT_MAX_ROWS;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcConnectionDetails jdbcConnectionDetails;

    private JdbcComponentHandlerImpl jdbcComponentHandler;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS poll_test;");
        jdbcTemplate.execute("CREATE TABLE poll_test (id INTEGER PRIMARY KEY, seq INTEGER);");

        jdbcComponentHandler = new JdbcComponentHandlerImpl(
            jdbcComponent("postgresql")
                .urlTemplate(jdbcConnectionDetails.getJdbcUrl())
                .jdbcDriverClassName("org.postgresql.Driver"));
    }

    @Test
    public void testFirstPollEmitsNothingAndNextPollEmitsNewRowsOldestFirst() {
        insertRows(1, 3);

        PollOutput firstPollOutput = poll("id", "DESC", Map.of());

        assertThat(firstPollOutput.records()).isEmpty();

        insertRows(4, 5);

        PollOutput secondPollOutput = poll("id", "DESC", firstPollOutput.closureParameters());

        assertThat(getValues(secondPollOutput, "id")).containsExactly(4, 5);
        assertThat(secondPollOutput.pollImmediately()).isFalse();

        PollOutput thirdPollOutput = poll("id", "DESC", secondPollOutput.closureParameters());

        assertThat(thirdPollOutput.records()).isEmpty();
    }

    @Test
    public void testPollWithAscendingDirectionTreatsSmallerValuesAsNewer() {
        insertRows(100, 102);

        PollOutput firstPollOutput = poll("id", "ASC", Map.of());

        assertThat(firstPollOutput.records()).isEmpty();

        insertRows(98, 99);

        PollOutput secondPollOutput = poll("id", "ASC", firstPollOutput.closureParameters());

        assertThat(getValues(secondPollOutput, "id")).containsExactly(99, 98);
    }

    @Test
    public void testPollCapsBatchAndNextPollContinuesAfterLastEmittedRow() {
        insertRows(0, 0);

        PollOutput firstPollOutput = poll("id", "DESC", Map.of());

        insertRows(1, BATCH_SIZE + 5);

        PollOutput secondPollOutput = poll("id", "DESC", firstPollOutput.closureParameters());

        assertThat(getValues(secondPollOutput, "id")).containsExactlyElementsOf(
            IntStream.rangeClosed(1, BATCH_SIZE)
                .boxed()
                .toList());
        assertThat(secondPollOutput.pollImmediately()).isTrue();

        PollOutput thirdPollOutput = poll("id", "DESC", secondPollOutput.closureParameters());

        assertThat(getValues(thirdPollOutput, "id")).containsExactly(
            BATCH_SIZE + 1, BATCH_SIZE + 2, BATCH_SIZE + 3, BATCH_SIZE + 4, BATCH_SIZE + 5);
        assertThat(thirdPollOutput.pollImmediately()).isFalse();
    }

    @Test
    public void testPollDoesNotSplitRowsSharingOrderValueAcrossBatches() {
        jdbcTemplate.execute("INSERT INTO poll_test VALUES (0, 0)");

        PollOutput firstPollOutput = poll("seq", "DESC", Map.of());

        jdbcTemplate.execute(
            "INSERT INTO poll_test SELECT i, i FROM generate_series(1, " + (BATCH_SIZE - 2) + ") AS i");
        jdbcTemplate.execute(
            "INSERT INTO poll_test SELECT i, " + (BATCH_SIZE - 1) + " FROM generate_series(" + (BATCH_SIZE - 1) +
                ", " + (BATCH_SIZE + 3) + ") AS i");

        PollOutput secondPollOutput = poll("seq", "DESC", firstPollOutput.closureParameters());

        assertThat(secondPollOutput.records()).hasSize(BATCH_SIZE - 2);
        assertThat(secondPollOutput.pollImmediately()).isTrue();

        PollOutput thirdPollOutput = poll("seq", "DESC", secondPollOutput.closureParameters());

        assertThat(getValues(thirdPollOutput, "id")).containsExactlyInAnyOrder(
            BATCH_SIZE - 1, BATCH_SIZE, BATCH_SIZE + 1, BATCH_SIZE + 2, BATCH_SIZE + 3);
    }

    @Test
    public void testPollOnEmptyTableEmitsRowsInsertedLater() {
        PollOutput firstPollOutput = poll("id", "DESC", Map.of());

        assertThat(firstPollOutput.records()).isEmpty();

        insertRows(1, 2);

        PollOutput secondPollOutput = poll("id", "DESC", firstPollOutput.closureParameters());

        assertThat(getValues(secondPollOutput, "id")).containsExactly(1, 2);
    }

    @Test
    public void testPollResumesFromNewestRowOfLegacyClosure() {
        insertRows(1, 5);

        Map<String, Object> legacyClosureParameters = Map.of(
            JdbcConstants.ALL_ROWS, List.of(Map.of("id", 3, "seq", 3), Map.of("id", 2, "seq", 2)),
            JdbcConstants.LAST_ITEM, 2);

        PollOutput pollOutput = poll("id", "DESC", legacyClosureParameters);

        assertThat(getValues(pollOutput, "id")).containsExactly(4, 5);
        assertThat(pollOutput.closureParameters()).doesNotContainKey(JdbcConstants.ALL_ROWS);
    }

    private void insertRows(int from, int to) {
        jdbcTemplate.update("INSERT INTO poll_test SELECT i, i FROM generate_series(?, ?) AS i", from, to);
    }

    private PollOutput poll(String orderBy, String orderDirection, Map<String, ?> closureParameters) {
        return jdbcComponentHandler.poll(
            ParametersFactory.create(
                Map.of(
                    JdbcConstants.SCHEMA, "public", JdbcConstants.TABLE, "poll_test", JdbcConstants.ORDER_BY, orderBy,
                    JdbcConstants.ORDER_DIRECTION, orderDirection)),
            ParametersFactory.create(
                Map.of(
                    JdbcConstants.HOST, "", JdbcConstants.DATABASE, "",
                    JdbcConstants.USERNAME, jdbcConnectionDetails.getUsername(),
                    JdbcConstants.PASSWORD, jdbcConnectionDetails.getPassword())),
            ParametersFactory.create(closureParameters), mock(TriggerContext.class));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getValues(PollOutput pollOutput, String columnName) {
        return pollOutput.records()
            .stream()
            .map(record -> ((Map<String, Object>) record).get(columnName))
            .toList();
    }
}
