
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

import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import com.bytechef.hermes.component.util.MapValueUtils;
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
        Connection connection, String databaseJdbcName, String jdbcDriverClassName) {

        Assert.notNull(connection, "'connection' must not be null");
        Assert.notNull(databaseJdbcName, "'databaseJdbcName' must not be null");
        Assert.notNull(jdbcDriverClassName, "'jdbcDriverClassName' must not be null");

        Map<String, Object> connectionInputParameters = connection.getParameters();

        String url = "jdbc:" + databaseJdbcName + "://"
            + MapValueUtils.getString(connectionInputParameters, JdbcConstants.HOST)
            + ":"
            + MapValueUtils.getString(connectionInputParameters, JdbcConstants.PORT)
            + "/"
            + MapValueUtils.getString(connectionInputParameters, JdbcConstants.DATABASE);
        String username = MapValueUtils.getString(connectionInputParameters, JdbcConstants.USERNAME);

        return dataSourceMap.computeIfAbsent(url + username, key -> {
            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

            dataSourceBuilder.driverClassName(jdbcDriverClassName);
            dataSourceBuilder.url(url);
            dataSourceBuilder.username(username);
            dataSourceBuilder.password(MapValueUtils.getString(connectionInputParameters, JdbcConstants.PASSWORD));

            return dataSourceBuilder.build();
        });
    }
}
