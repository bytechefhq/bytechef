
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

package com.bytechef.server.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.data.storage.db.repository.converter.DataEntryValueWrapperToStringConverter;
import com.bytechef.data.storage.db.repository.converter.StringToDataEntryValueWrapperConverter;
import com.bytechef.encryption.Encryption;
import com.bytechef.hermes.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.hermes.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.hermes.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.hermes.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.hermes.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.hermes.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
            new DataEntryValueWrapperToStringConverter(objectMapper),
            new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
            new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
            new ExecutionErrorToStringConverter(objectMapper),
            new FileEntryToStringConverter(objectMapper),
            new MapWrapperToStringConverter(objectMapper),
            new StringToDataEntryValueWrapperConverter(objectMapper),
            new StringToExecutionErrorConverter(objectMapper),
            new StringToFileEntryConverter(objectMapper),
            new StringToMapWrapperConverter(objectMapper),
            new StringToWebhooksConverter(objectMapper),
            new StringToWorkflowExecutionIdConverter(),
            new StringToWorkflowTaskConverter(objectMapper),
            new StringToWorkflowTriggerConverter(objectMapper),
            new StringToTriggerStateValueConverter(objectMapper),
            new TriggerStateValueToStringConverter(objectMapper),
            new WebhooksToStringConverter(objectMapper),
            new WorkflowExecutionIdToStringConverter(),
            new WorkflowTaskToStringConverter(objectMapper),
            new WorkflowTriggerToStringConverter(objectMapper));
    }
}
