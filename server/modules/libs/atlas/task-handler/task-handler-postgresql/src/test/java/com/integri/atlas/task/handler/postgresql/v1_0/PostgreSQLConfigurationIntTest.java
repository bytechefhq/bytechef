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

package com.integri.atlas.task.handler.postgresql.v1_0;

import static com.integri.atlas.engine.Constants.AUTH;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.DATABASE;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.HOST;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.PASSWORD;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.PORT;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.USERNAME;

import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.task.jdbc.commons.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class PostgreSQLConfigurationIntTest {

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Test
    public void testDataSourceFactory() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(
            AUTH,
            Map.of(HOST, "host", PORT, 1234, DATABASE, "database", USERNAME, "username", PASSWORD, "password")
        );

        HikariDataSource dataSource = (HikariDataSource) dataSourceFactory.createDataSource(taskExecution);

        Assertions.assertEquals("org.postgresql.Driver", dataSource.getDriverClassName());
        Assertions.assertEquals("jdbc:postgresql://host:1234/database", dataSource.getJdbcUrl());
        Assertions.assertEquals("password", dataSource.getPassword());
        Assertions.assertEquals("username", dataSource.getUsername());
    }
}
