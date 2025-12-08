/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.jdbc;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import java.util.Map;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class DataSourceFactory {

    public static SingleConnectionDataSource getDataSource(
        Map<String, ?> connectionParameters, String urlTemplate, String jdbcDriverClassName) {

        Assert.notNull(urlTemplate, "'urlTemplate' must not be null");
        Assert.notNull(jdbcDriverClassName, "'jdbcDriverClassName' must not be null");

        String url = urlTemplate
            .replace("{host}", MapUtils.getString(connectionParameters, JdbcConstants.HOST))
            .replace("{port}", MapUtils.getString(connectionParameters, JdbcConstants.PORT))
            .replace("{database}", MapUtils.getString(connectionParameters, JdbcConstants.DATABASE));
        String username = MapUtils.getString(connectionParameters, JdbcConstants.USERNAME);
        String password = MapUtils.getString(connectionParameters, JdbcConstants.PASSWORD);

        return new SingleConnectionDataSource(url, username, password, false);
    }
}
