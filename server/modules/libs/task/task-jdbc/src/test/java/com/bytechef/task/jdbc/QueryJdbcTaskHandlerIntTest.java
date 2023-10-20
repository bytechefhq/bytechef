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
import java.util.List;
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
public class QueryJdbcTaskHandlerIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private QueryJdbcTaskHandler queryJdbcTaskHandler;

    @BeforeEach
    public void beforeEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
                """
            DROP TABLE IF EXISTS test;
            CREATE TABLE IF NOT EXISTS test (
                id   varchar(256) not null primary key,
                name varchar(256) not null
            );
            INSERT INTO test VALUES('id1', 'name1');
            INSERT INTO test VALUES('id2', 'name2');
            INSERT INTO test VALUES('id3', 'name3');
            INSERT INTO test VALUES('id4', 'name4');
        """);
    }

    @Test
    public void testQuery() {
        TaskExecution taskExecution = new SimpleTaskExecution(
                Map.of("query", "SELECT count(*) FROM test where id=:id", "parameters", Map.of("id", "id2")));

        queryJdbcTaskHandler.handle(taskExecution);

        List<Map<String, ?>> result = queryJdbcTaskHandler.handle(taskExecution);

        Assertions.assertEquals(1, result.size());
    }

    @TestConfiguration
    public static class InsertJdbcTaskHandlerIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        QueryJdbcTaskHandler queryJdbcTaskHandler(PlatformTransactionManager transactionManager) {
            return new QueryJdbcTaskHandler(taskExecution -> dataSource, transactionManager);
        }
    }
}
