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

package com.integri.atlas.task.handler.mysql.v1_0;

import static com.integri.atlas.engine.Constants.AUTH;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.DATABASE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.HOST;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PASSWORD;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PORT;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.USERNAME;

import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.task.commons.jdbc.DataSourceFactory;
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
public class MySQLConfigurationIntTest {

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

        Assertions.assertEquals("com.mysql.jdbc.Driver", dataSource.getDriverClassName());
        Assertions.assertEquals("jdbc:mysql://host:1234/database", dataSource.getJdbcUrl());
        Assertions.assertEquals("password", dataSource.getPassword());
        Assertions.assertEquals("username", dataSource.getUsername());
    }
}
