
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

package com.bytechef.hermes.component.registrar.jdbc;

import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Component
public class DataSourceFactory {
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    public DataSource getDataSource(
        InputParameters connectionParameters, String databaseJdbcName, String jdbcDriverClassName) {

        Assert.notNull(connectionParameters, "'connectionParameters' must not be null");
        Assert.notNull(databaseJdbcName, "'databaseJdbcName' must not be null");
        Assert.notNull(jdbcDriverClassName, "'jdbcDriverClassName' must not be null");

        String url = "jdbc:" + databaseJdbcName + "://"
            + connectionParameters.getString(JdbcConstants.HOST)
            + ":"
            + connectionParameters.getString(JdbcConstants.PORT)
            + "/"
            + connectionParameters.getString(JdbcConstants.DATABASE);
        String username = connectionParameters.getString(JdbcConstants.USERNAME);

        return dataSourceMap.computeIfAbsent(url + username, key -> {
            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

            dataSourceBuilder.driverClassName(jdbcDriverClassName);
            dataSourceBuilder.url(url);
            dataSourceBuilder.username(username);
            dataSourceBuilder.password(connectionParameters.getString(JdbcConstants.PASSWORD));

            return dataSourceBuilder.build();
        });
    }
}
