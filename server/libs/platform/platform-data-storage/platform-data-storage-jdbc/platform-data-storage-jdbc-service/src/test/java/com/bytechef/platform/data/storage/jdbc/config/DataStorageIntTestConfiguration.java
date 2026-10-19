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

package com.bytechef.platform.data.storage.jdbc.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.data.storage.jdbc.repository.converter.DataEntryValueWrapperToStringConverter;
import com.bytechef.platform.data.storage.jdbc.repository.converter.StringToDataEntryValueWrapperConverter;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class DataStorageIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class DataStorageIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        @Override
        protected List<?> userConverters() {
            return List.of(new DataEntryValueWrapperToStringConverter(), new StringToDataEntryValueWrapperConverter());
        }
    }
}
