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

package com.bytechef.test.config.h2;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Ivica Cardic
 */
@TestConfiguration(proxyBeanMethods = false)
public class H2DataSourceConfiguration {

    @Bean
    public DataSource dataSource() {
        UUID uuid = UUID.randomUUID();

        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + uuid + ";DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=KEY,VALUE,USER",
            "sa", "");

        driverManagerDataSource.setDriverClassName("org.h2.Driver");

        return driverManagerDataSource;
    }
}
