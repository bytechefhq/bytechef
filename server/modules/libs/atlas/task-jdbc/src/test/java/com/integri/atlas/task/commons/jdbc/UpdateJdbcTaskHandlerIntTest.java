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

package com.integri.atlas.task.commons.jdbc;

import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
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
public class UpdateJdbcTaskHandlerIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UpdateJdbcTaskHandler updateJdbcTaskHandler;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
            """
            DROP TABLE IF EXISTS test;
            CREATE TABLE test (
                id   varchar(256) not null primary key,
                name varchar(256) not null
            );
            INSERT INTO test VALUES('id1', 'name1');
            INSERT INTO test VALUES('id2', 'name2');
            INSERT INTO test VALUES('id3', 'name3');
            INSERT INTO test VALUES('id4', 'name4');
        """
        );
    }

    @Test
    public void testUpdate() throws Exception {
        TaskExecution taskExecution = new SimpleTaskExecution(
            Map.of("table", "test", "columns", List.of("name"), "rows", List.of(Map.of("id", "id2", "name", "name3")))
        );

        Map<String, Integer> result = updateJdbcTaskHandler.handle(taskExecution);

        Assertions.assertEquals(1, result.get("rows"));

        Assertions.assertEquals(
            "name3",
            jdbcTemplate.queryForObject("SELECT name FROM test WHERE id='id2'", String.class)
        );
    }

    @TestConfiguration
    public static class UpdateJdbcTaskHandlerIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        UpdateJdbcTaskHandler updateJdbcTaskHandler(PlatformTransactionManager transactionManager) {
            return new UpdateJdbcTaskHandler(taskExecution -> dataSource, transactionManager);
        }
    }
}
