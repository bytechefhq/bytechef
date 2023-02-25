
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

package com.bytechef.coordinator.config;

import com.bytechef.atlas.repository.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.atlas.repository.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.atlas.repository.jdbc.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.repository.jdbc.converter.WorkflowTaskToStringConverter;
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
//@ConditionalOnProperty(prefix = "bytechef.workflow", name = "persistence.provider", havingValue = "jdbc")
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
            new ExecutionErrorToStringConverter(objectMapper),
            new StringToExecutionErrorConverter(objectMapper),
            new StringToWorkflowTaskConverter(objectMapper),
            new WorkflowTaskToStringConverter(objectMapper));
    }
}
