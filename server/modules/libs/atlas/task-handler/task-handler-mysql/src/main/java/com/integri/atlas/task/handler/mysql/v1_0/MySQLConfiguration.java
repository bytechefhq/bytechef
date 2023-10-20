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
import static com.integri.atlas.task.handler.mysql.MySQLTaskConstants.MYSQL;
import static com.integri.atlas.task.handler.mysql.MySQLTaskConstants.VERSION_1_0;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.DATABASE;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.DELETE;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.EXECUTE;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.HOST;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.INSERT;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.PASSWORD;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.PORT;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.QUERY;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.UPDATE;
import static com.integri.atlas.task.jdbc.commons.JdbcTaskConstants.USERNAME;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.task.jdbc.commons.DataSourceFactory;
import com.integri.atlas.task.jdbc.commons.DeleteJdbcTaskHandler;
import com.integri.atlas.task.jdbc.commons.ExecuteJdbcTaskHandler;
import com.integri.atlas.task.jdbc.commons.InsertJdbcTaskHandler;
import com.integri.atlas.task.jdbc.commons.QueryJdbcTaskHandler;
import com.integri.atlas.task.jdbc.commons.UpdateJdbcTaskHandler;
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

    private final Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Bean(MYSQL + "/" + VERSION_1_0 + "/" + DELETE)
    DeleteJdbcTaskHandler deleteJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new DeleteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MYSQL + "/" + VERSION_1_0 + "/" + EXECUTE)
    ExecuteJdbcTaskHandler executeExecuteTaskHandler(PlatformTransactionManager transactionManager) {
        return new ExecuteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MYSQL + "/" + VERSION_1_0 + "/" + INSERT)
    InsertJdbcTaskHandler insertJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new InsertJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MYSQL + "/" + VERSION_1_0 + "/" + QUERY)
    QueryJdbcTaskHandler queryJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new QueryJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MYSQL + "/" + VERSION_1_0 + "/" + UPDATE)
    UpdateJdbcTaskHandler updateJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new UpdateJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MYSQL + "DataSourceFactory")
    DataSourceFactory dataSourceFactory() {
        return taskExecution -> {
            DataSource dataSource = null;

            if (taskExecution.containsKey(AUTH)) {
                Accessor taskAuthAccessor = new MapObject(taskExecution.getMap(AUTH));

                String url =
                    "jdbc:mysql://" +
                    taskAuthAccessor.getString(HOST) +
                    ":" +
                    taskAuthAccessor.getString(PORT) +
                    "/" +
                    taskAuthAccessor.getString(DATABASE);
                String username = taskAuthAccessor.getString(USERNAME);
                String password = taskAuthAccessor.getString(PASSWORD);

                dataSource =
                    dataSourceMap.computeIfAbsent(
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
            }

            return dataSource;
        };
    }
}
