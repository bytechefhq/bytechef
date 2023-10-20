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

package com.integri.atlas.task.handler.mysql;

import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_DELETE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_EXECUTE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_INSERT;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_QUERY;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_UPDATE;
import static com.integri.atlas.task.handler.mysql.MySQLTaskConstants.TASK_MYSQL;

import com.integri.atlas.task.commons.jdbc.DataSourceFactory;
import com.integri.atlas.task.commons.jdbc.DeleteJdbcTaskHandler;
import com.integri.atlas.task.commons.jdbc.ExecuteJdbcTaskHandler;
import com.integri.atlas.task.commons.jdbc.InsertJdbcTaskHandler;
import com.integri.atlas.task.commons.jdbc.QueryJdbcTaskHandler;
import com.integri.atlas.task.commons.jdbc.UpdateJdbcTaskHandler;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
@Configuration
public class MySQLConfiguration {

    private Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Bean(TASK_MYSQL + "/" + OPERATION_DELETE)
    DeleteJdbcTaskHandler deleteJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new DeleteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_MYSQL + "/" + OPERATION_EXECUTE)
    ExecuteJdbcTaskHandler executeExecuteTaskHandler(PlatformTransactionManager transactionManager) {
        return new ExecuteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_MYSQL + "/" + OPERATION_INSERT)
    InsertJdbcTaskHandler insertJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new InsertJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_MYSQL + "/" + OPERATION_QUERY)
    QueryJdbcTaskHandler queryJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new QueryJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_MYSQL + "/" + OPERATION_UPDATE)
    UpdateJdbcTaskHandler updateJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new UpdateJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_MYSQL + "DataSourceFactory")
    DataSourceFactory dataSourceFactory() {
        return taskExecution -> {
            String url = "jdbc:postgresql://localhost/test";
            String username = "root";
            String password = "root";

            return dataSourceMap.computeIfAbsent(
                url + username + password,
                key -> {
                    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

                    dataSourceBuilder.driverClassName("com.mysql.jdbc.Driver");
                    dataSourceBuilder.url(url);
                    dataSourceBuilder.username(username);
                    dataSourceBuilder.password(password);

                    return dataSourceBuilder.build();
                }
            );
        };
    }
}
