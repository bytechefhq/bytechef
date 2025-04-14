/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.config;

import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Matija Petanjek
 */
@ComponentScan(basePackages = {
    "com.bytechef.platform.configuration"
}, excludeFilters = @ComponentScan.Filter(
    type = FilterType.REGEX,
    pattern = {
        "com\\.bytechef\\.platform\\.configuration\\.facade\\..*",
        "com\\.bytechef\\.platform\\.configuration\\.event\\..*",
        "com\\.bytechef\\.platform\\.configuration\\.workflow\\..*"
    }))
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@EnableAutoConfiguration
@Configuration
public class NotificationIntTestConfiguration {

    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.platform.configuration.repository"
        })
    public static class IntegrationIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public IntegrationIntTestJdbcConfiguration(
            @Qualifier("objectMapper") ObjectMapper objectMapper) {

            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new MapWrapperToStringConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
