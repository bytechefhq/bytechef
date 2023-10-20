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
import com.integri.atlas.task.jdbc.commons.InsertJdbcTaskHandler;
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
public class InsertJdbcTaskHandlerIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InsertJdbcTaskHandler insertJdbcTaskHandler;

    @BeforeEach
    public void beforeEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
            """
            DROP TABLE IF EXISTS test;
            CREATE TABLE test (
                id   varchar(256) not null primary key,
                name varchar(256) not null
            );
        """
        );
    }

    @Test
    public void testInsert() throws Exception {
        TaskExecution taskExecution = new SimpleTaskExecution(
            Map.of(
                "table",
                "test",
                "columns",
                List.of("id", "name"),
                "rows",
                List.of(Map.of("id", "id1", "name", "name1"), Map.of("id", "id2", "name", "name2"))
            )
        );

        Map<String, Integer> result = insertJdbcTaskHandler.handle(taskExecution);

        Assertions.assertEquals(2, result.get("rows"));
    }

    @TestConfiguration
    public static class InsertJdbcTaskHandlerIntTestConfiguration {

        @Autowired
        private DataSource dataSource;

        @Bean
        InsertJdbcTaskHandler insertJdbcTaskHandler(PlatformTransactionManager transactionManager) {
            return new InsertJdbcTaskHandler(taskExecution -> dataSource, transactionManager);
        }
    }
}
