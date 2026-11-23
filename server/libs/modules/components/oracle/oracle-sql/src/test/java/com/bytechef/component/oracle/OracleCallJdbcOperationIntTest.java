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

package com.bytechef.component.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.operation.CallJdbcOperation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.oracle.OracleContainer;

/**
 * @author Igor Beslic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class OracleCallJdbcOperationIntTest {

    private static final OracleContainer ORACLE_CONTAINER = new OracleContainer(
        "gvenzl/oracle-free:23.26.4-slim-faststart");

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void beforeAll() {
        ORACLE_CONTAINER.start();

        dataSource = new DriverManagerDataSource(
            ORACLE_CONTAINER.getJdbcUrl(), ORACLE_CONTAINER.getUsername(), ORACLE_CONTAINER.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
            """
                CREATE OR REPLACE FUNCTION call_balance(account_id IN NUMBER) RETURN NUMBER AS
                    account_balance NUMBER;
                BEGIN
                    SELECT balance INTO account_balance FROM call_account WHERE id = account_id;

                    RETURN account_balance;
                END;
                """);

        jdbcTemplate.execute(
            """
                CREATE OR REPLACE PROCEDURE call_transfer(
                    from_id IN NUMBER, to_id IN NUMBER, amount IN NUMBER, note IN OUT VARCHAR2,
                    remaining OUT NUMBER) AS
                BEGIN
                    UPDATE call_account SET balance = balance - amount WHERE id = from_id;
                    UPDATE call_account SET balance = balance + amount WHERE id = to_id;

                    SELECT balance INTO remaining FROM call_account WHERE id = from_id;

                    note := note || ' done';
                END;
                """);
    }

    @AfterAll
    public static void afterAll() {
        ORACLE_CONTAINER.stop();
    }

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS call_account");
        jdbcTemplate.execute("CREATE TABLE call_account (id NUMBER PRIMARY KEY, balance NUMBER(12, 2) NOT NULL)");
        jdbcTemplate.execute("INSERT INTO call_account VALUES (1, 120.50)");
        jdbcTemplate.execute("INSERT INTO call_account VALUES (2, 10.00)");
    }

    @Test
    public void testCallFunctionReturnsValue() {
        Map<String, ?> nameInputParameters = Map.of(
            JdbcConstants.CALL, "{:balance = call call_balance(:accountId)}",
            JdbcConstants.PARAMETERS, Map.of("accountId", 1),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "NUMERIC", "mode", "OUT")));

        Map<String, Object> nameOutputValues = new CallJdbcOperation().execute(nameInputParameters, dataSource);

        assertThat(nameOutputValues).containsOnlyKeys("balance");
        assertThat((BigDecimal) nameOutputValues.get("balance")).isEqualByComparingTo("120.50");
    }

    @Test
    public void testCallFunctionReturnsFloatValue() {
        Map<String, ?> nameInputParameters = Map.of(
            JdbcConstants.CALL, "{:balance = call call_balance(:accountId)}",
            JdbcConstants.PARAMETERS, Map.of("accountId", 1),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "FLOAT", "mode", "OUT")));

        Map<String, Object> nameOutputValues = new CallJdbcOperation().execute(nameInputParameters, dataSource);

        assertThat(nameOutputValues.get("balance")).isInstanceOf(Number.class);
        assertThat(((Number) nameOutputValues.get("balance")).doubleValue()).isEqualTo(120.5);
    }

    @Test
    public void testCallProcedureWithOutAndInOutParameters() {
        Map<String, ?> nameInputParameters = Map.of(
            JdbcConstants.CALL, "{call call_transfer(:from, :to, :amount, :note, :remaining)}",
            JdbcConstants.PARAMETERS, Map.of("amount", 20.5, "from", 1, "note", "transfer", "to", 2),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(
                Map.of("name", "note", "type", "VARCHAR", "mode", "IN_OUT"),
                Map.of("name", "remaining", "type", "NUMERIC", "mode", "OUT")));

        Map<String, Object> nameOutputValues = new CallJdbcOperation().execute(nameInputParameters, dataSource);

        assertThat(nameOutputValues).containsEntry("note", "transfer done");
        assertThat((BigDecimal) nameOutputValues.get("remaining")).isEqualByComparingTo("100.00");
        assertThat(jdbcTemplate.queryForObject("SELECT balance FROM call_account WHERE id = 2", BigDecimal.class))
            .isEqualByComparingTo("30.50");
    }

    @Test
    public void testCallAnonymousPlSqlBlock() {
        Map<String, ?> nameInputParameters = Map.of(
            JdbcConstants.CALL, "BEGIN :balance := call_balance(:accountId); END;",
            JdbcConstants.PARAMETERS, Map.of("accountId", 2),
            JdbcConstants.OUTPUT_PARAMETERS, List.of(Map.of("name", "balance", "type", "NUMERIC", "mode", "OUT")));

        Map<String, Object> nameOutputValues = new CallJdbcOperation().execute(nameInputParameters, dataSource);

        assertThat((BigDecimal) nameOutputValues.get("balance")).isEqualByComparingTo("10.00");
    }
}
