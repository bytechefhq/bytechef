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

package com.bytechef.platform.scheduler.config;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Test configuration for Quartz JDBC integration tests with PostgreSQL. Unlike QuartzTriggerSchedulerTestConfiguration,
 * this does NOT exclude DataSourceAutoConfiguration so that testcontainers can wire the DataSource.
 *
 * @author Ivica Cardic
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    HibernateJpaAutoConfiguration.class
})
public class QuartzJdbcTestConfiguration {

    public QuartzJdbcTestConfiguration() {
        JsonUtils.setObjectMapper(new ObjectMapper());
    }
}
