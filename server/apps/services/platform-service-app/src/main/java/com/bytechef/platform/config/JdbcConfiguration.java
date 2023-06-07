
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

package com.bytechef.platform.config;

import com.bytechef.atlas.execution.repository.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.configuration.repository.jdbc.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.configuration.repository.jdbc.converter.WorkflowTaskToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.encryption.Encryption;
import com.bytechef.hermes.configuration.converter.StringToTriggerLifecycleValueConverter;
import com.bytechef.hermes.configuration.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.hermes.configuration.converter.StringToWorkflowTriggerConverter;
import com.bytechef.hermes.configuration.converter.TriggerLifecycleValueToStringConverter;
import com.bytechef.hermes.configuration.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.hermes.configuration.converter.WorkflowTriggerToStringConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
@EnableJdbcRepositories(basePackages = "com.bytechef")
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Bean
    AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return CurrentDateTimeProvider.INSTANCE;
    }

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
            new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
            new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
            new ExecutionErrorToStringConverter(objectMapper),
            new MapWrapperToStringConverter(objectMapper),
            new StringToExecutionErrorConverter(objectMapper),
            new StringToMapWrapperConverter(objectMapper),
            new StringToWebhooksConverter(objectMapper),
            new StringToWorkflowExecutionIdConverter(),
            new StringToWorkflowTaskConverter(objectMapper),
            new StringToWorkflowTriggerConverter(objectMapper),
            new StringToTriggerLifecycleValueConverter(objectMapper),
            new TriggerLifecycleValueToStringConverter(objectMapper),
            new WebhooksToStringConverter(objectMapper),
            new WorkflowExecutionIdToStringConverter(),
            new WorkflowTaskToStringConverter(objectMapper),
            new WorkflowTriggerToStringConverter(objectMapper));
    }
}
