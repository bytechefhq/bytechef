
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

package com.bytechef.hermes.component.jdbc.sql;

import com.bytechef.hermes.component.definition.Context.Connection;
import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import com.bytechef.commons.util.MapUtils;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class DataSourceFactory {

    private static final Map<String, DataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    public DataSource getDataSource(
        Connection connection, String databaseJdbcName, String jdbcDriverClassName) {

        Assert.notNull(connection, "'connection' must not be null");
        Assert.notNull(databaseJdbcName, "'databaseJdbcName' must not be null");
        Assert.notNull(jdbcDriverClassName, "'jdbcDriverClassName' must not be null");

        Map<String, Object> connectionParameters = connection.getParameters();

        String url = "jdbc:" + databaseJdbcName + "://"
            + MapUtils.getString(connectionParameters, JdbcConstants.HOST)
            + ":"
            + MapUtils.getString(connectionParameters, JdbcConstants.PORT)
            + "/"
            + MapUtils.getString(connectionParameters, JdbcConstants.DATABASE);
        String username = MapUtils.getString(connectionParameters, JdbcConstants.USERNAME);

        return DATA_SOURCE_MAP.computeIfAbsent(url + username, key -> {
            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

            dataSourceBuilder.driverClassName(jdbcDriverClassName);
            dataSourceBuilder.url(url);
            dataSourceBuilder.username(username);
            dataSourceBuilder.password(MapUtils.getString(connectionParameters, JdbcConstants.PASSWORD));

            return dataSourceBuilder.build();
        });
    }
}
