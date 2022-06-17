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

package com.bytechef.task.handler.mysql.v1_0;

import static com.bytechef.hermes.auth.AuthenticationConstants.*;
import static com.bytechef.task.jdbc.JdbcTaskConstants.DATABASE;
import static com.bytechef.task.jdbc.JdbcTaskConstants.DELETE;
import static com.bytechef.task.jdbc.JdbcTaskConstants.EXECUTE;
import static com.bytechef.task.jdbc.JdbcTaskConstants.HOST;
import static com.bytechef.task.jdbc.JdbcTaskConstants.INSERT;
import static com.bytechef.task.jdbc.JdbcTaskConstants.PASSWORD;
import static com.bytechef.task.jdbc.JdbcTaskConstants.PORT;
import static com.bytechef.task.jdbc.JdbcTaskConstants.QUERY;
import static com.bytechef.task.jdbc.JdbcTaskConstants.UPDATE;
import static com.bytechef.task.jdbc.JdbcTaskConstants.USERNAME;

import com.bytechef.atlas.Accessor;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.service.AuthenticationService;
import com.bytechef.task.handler.mysql.MySQLTaskConstants;
import com.bytechef.task.jdbc.DataSourceFactory;
import com.bytechef.task.jdbc.DeleteJdbcTaskHandler;
import com.bytechef.task.jdbc.ExecuteJdbcTaskHandler;
import com.bytechef.task.jdbc.InsertJdbcTaskHandler;
import com.bytechef.task.jdbc.QueryJdbcTaskHandler;
import com.bytechef.task.jdbc.UpdateJdbcTaskHandler;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
@Configuration
public class MySQLConfiguration {

    @Autowired
    private AuthenticationService authenticationService;

    private final Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Bean(MySQLTaskConstants.MYSQL + "/" + MySQLTaskConstants.VERSION_1_0 + "/" + DELETE)
    DeleteJdbcTaskHandler deleteJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new DeleteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MySQLTaskConstants.MYSQL + "/" + MySQLTaskConstants.VERSION_1_0 + "/" + EXECUTE)
    ExecuteJdbcTaskHandler executeExecuteTaskHandler(PlatformTransactionManager transactionManager) {
        return new ExecuteJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MySQLTaskConstants.MYSQL + "/" + MySQLTaskConstants.VERSION_1_0 + "/" + INSERT)
    InsertJdbcTaskHandler insertJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new InsertJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MySQLTaskConstants.MYSQL + "/" + MySQLTaskConstants.VERSION_1_0 + "/" + QUERY)
    QueryJdbcTaskHandler queryJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new QueryJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MySQLTaskConstants.MYSQL + "/" + MySQLTaskConstants.VERSION_1_0 + "/" + UPDATE)
    UpdateJdbcTaskHandler updateJdbcTaskHandler(PlatformTransactionManager transactionManager) {
        return new UpdateJdbcTaskHandler(dataSourceFactory(), transactionManager);
    }

    @Bean(MySQLTaskConstants.MYSQL + "DataSourceFactory")
    DataSourceFactory dataSourceFactory() {
        return taskExecution -> {
            DataSource dataSource = null;

            if (taskExecution.containsKey(AUTHENTICATION_ID)) {
                Authentication authentication =
                        authenticationService.fetchAuthentication(taskExecution.getString(AUTHENTICATION_ID));

                Accessor authenticationAccessor = authentication.getProperties();

                String url = "jdbc:mysql://"
                        + authenticationAccessor.getString(HOST)
                        + ":"
                        + authenticationAccessor.getString(PORT)
                        + "/"
                        + authenticationAccessor.getString(DATABASE);
                String username = authenticationAccessor.getString(USERNAME);
                String password = authenticationAccessor.getString(PASSWORD);

                dataSource = dataSourceMap.computeIfAbsent(url + username + password, key -> {
                    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

                    dataSourceBuilder.driverClassName("com.mysql.jdbc.Driver");
                    dataSourceBuilder.url(url);
                    dataSourceBuilder.username(username);
                    dataSourceBuilder.password(password);

                    return dataSourceBuilder.build();
                });
            }

            return dataSource;
        };
    }
}
