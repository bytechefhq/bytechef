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

package com.integri.atlas.task.handler.postgresql;

import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_DELETE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_EXECUTE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_INSERT;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_QUERY;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.OPERATION_UPDATE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_DATABASE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_HOST;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_PASSWORD;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_PORT;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_USERNAME;
import static com.integri.atlas.task.handler.postgresql.PostgreSQLTaskConstants.TASK_POSTGRESQL;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.MapObject;
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
public class PostgreSQLConfiguration {

    private final Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Bean(TASK_POSTGRESQL + "/" + OPERATION_DELETE)
    DeleteJdbcTaskHandler deleteJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new DeleteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_POSTGRESQL + "/" + OPERATION_EXECUTE)
    ExecuteJdbcTaskHandler executeExecuteTaskHandler(PlatformTransactionManager transactionManager) {
        return new ExecuteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_POSTGRESQL + "/" + OPERATION_INSERT)
    InsertJdbcTaskHandler insertJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new InsertJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_POSTGRESQL + "/" + OPERATION_QUERY)
    QueryJdbcTaskHandler queryJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new QueryJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_POSTGRESQL + "/" + OPERATION_UPDATE)
    UpdateJdbcTaskHandler updateJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new UpdateJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(TASK_POSTGRESQL + "DataSourceFactory")
    DataSourceFactory dataSourceFactory() {
        return taskExecution -> {
            DataSource dataSource = null;

            if (taskExecution.containsKey(Constants.AUTH)) {
                Accessor taskAuthAccessor = new MapObject(taskExecution.getMap(Constants.AUTH));

                String url =
                    "jdbc:postgresql://" +
                    taskAuthAccessor.getString(PROPERTY_HOST) +
                    ":" +
                    taskAuthAccessor.getString(PROPERTY_PORT) +
                    "/" +
                    taskAuthAccessor.getString(PROPERTY_DATABASE);
                String username = taskAuthAccessor.getString(PROPERTY_USERNAME);
                String password = taskAuthAccessor.getString(PROPERTY_PASSWORD);

                dataSource =
                    dataSourceMap.computeIfAbsent(
                        url + username + password,
                        key -> {
                            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

                            dataSourceBuilder.driverClassName("org.postgresql.Driver");
                            dataSourceBuilder.url(url);
                            dataSourceBuilder.username(username);
                            dataSourceBuilder.password(password);

                            return dataSourceBuilder.build();
                        }
                    );
            }

            return dataSource;
        };
    }
}
