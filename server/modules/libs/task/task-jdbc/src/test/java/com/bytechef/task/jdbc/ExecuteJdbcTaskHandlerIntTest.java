/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.jdbc;

import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class ExecuteJdbcTaskHandlerIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ExecuteJdbcTaskHandler executeJdbcTaskHandler;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");
    }

    @Test
    public void testExecute() throws Exception {
        TaskExecution taskExecution = new SimpleTaskExecution(
                Map.of(
                        "execute",
                        """
                    CREATE TABLE IF NOT EXISTS test (
                        id   varchar(256) not null primary key,
                        name varchar(256) not null
                    )
                """));

        executeJdbcTaskHandler.handle(taskExecution);

        Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));

        taskExecution = new SimpleTaskExecution(Map.of(
                "execute", "INSERT INTO test VALUES(:id, :name)", "parameters", Map.of("id", "id1", "name", "name1")));

        executeJdbcTaskHandler.handle(taskExecution);

        Assertions.assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM test", Integer.class));
    }

    @TestConfiguration
    public static class ExecuteJdbcTaskHandlerIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        ExecuteJdbcTaskHandler executeJdbcTaskHandler(PlatformTransactionManager transactionManager) {
            return new ExecuteJdbcTaskHandler(taskExecution -> dataSource, transactionManager);
        }
    }
}
