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

package com.bytechef.platform.component.jdbc.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Igor Beslic
 */
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
public class CallJdbcOperationIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS call_account");
        jdbcTemplate.execute("CREATE TABLE call_account (id integer primary key, balance numeric(12, 2) not null)");
        jdbcTemplate.execute("INSERT INTO call_account VALUES (1, 120.50), (2, 10.00)");

        jdbcTemplate.execute(
            """
                CREATE OR REPLACE FUNCTION call_balance(account_id integer) RETURNS numeric AS $$
                    SELECT balance FROM call_account WHERE id = account_id
                $$ LANGUAGE sql
                """);

        jdbcTemplate.execute(
            """
                CREATE OR REPLACE PROCEDURE call_transfer(
                    from_id integer, to_id integer, amount numeric, INOUT note varchar, OUT remaining numeric)
                AS $$
                BEGIN
                    UPDATE call_account SET balance = balance - amount WHERE id = from_id;
                    UPDATE call_account SET balance = balance + amount WHERE id = to_id;
                    SELECT balance INTO remaining FROM call_account WHERE id = from_id;
                    note := note || ' done';
                END
                $$ LANGUAGE plpgsql
                """);
    }

    @Test
    public void testCallFunctionReturnsValue() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.CALL, "{:balance = call call_balance(:accountId)}",
            JdbcConstants.PARAMETERS, Map.of("accountId", 1),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "NUMERIC", "mode", "OUT")));

        Map<String, Object> result = execute(inputParameters);

        assertThat(result).containsOnlyKeys("balance");
        assertThat((BigDecimal) result.get("balance")).isEqualByComparingTo("120.50");
    }

    @Test
    public void testCallProcedureWithOutAndInOutParameters() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.CALL, "call call_transfer(:from, :to, :amount, :note, :remaining)",
            JdbcConstants.PARAMETERS, Map.of("amount", 20.5, "from", 1, "note", "transfer", "to", 2),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(
                Map.of("name", "note", "type", "VARCHAR", "mode", "IN_OUT"),
                Map.of("name", "remaining", "type", "NUMERIC", "mode", "OUT")));

        Map<String, Object> result = execute(inputParameters);

        assertThat(result).containsEntry("note", "transfer done");
        assertThat((BigDecimal) result.get("remaining")).isEqualByComparingTo("100.00");
        assertThat(jdbcTemplate.queryForObject("SELECT balance FROM call_account WHERE id = 2", BigDecimal.class))
            .isEqualByComparingTo("30.50");
    }

    @Test
    public void testCallWithoutOutputParametersReturnsEmptyMap() throws SQLException {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.CALL, "{call call_balance(:accountId)}",
            JdbcConstants.PARAMETERS, Map.of("accountId", 2));

        assertThat(execute(inputParameters)).isEmpty();
    }

    @Test
    public void testMissingInputValueFails() {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.CALL, "{:balance = call call_balance(:accountId)}",
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "NUMERIC")));

        assertThatThrownBy(() -> execute(inputParameters))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(":accountId");
    }

    @Test
    public void testOutputParameterMissingFromStatementFails() {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.CALL, "{call call_balance(:accountId)}",
            JdbcConstants.PARAMETERS, Map.of("accountId", 1),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "NUMERIC")));

        assertThatThrownBy(() -> execute(inputParameters))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(":balance is not used");
    }

    @Test
    public void testUnsupportedOutputTypeFails() {
        Map<String, ?> inputParameters = Map.of(
            JdbcConstants.CALL, "{:balance = call call_balance(:accountId)}",
            JdbcConstants.PARAMETERS, Map.of("accountId", 1),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "BLOB")));

        assertThatThrownBy(() -> execute(inputParameters))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Unsupported output parameter type: BLOB");
    }

    private Map<String, Object> execute(Map<String, ?> inputParameters) throws SQLException {
        try (SingleConnectionDataSource singleConnectionDataSource =
            new SingleConnectionDataSource(dataSource.getConnection(), false)) {

            return new CallJdbcOperation().execute(inputParameters, singleConnectionDataSource);
        }
    }
}
