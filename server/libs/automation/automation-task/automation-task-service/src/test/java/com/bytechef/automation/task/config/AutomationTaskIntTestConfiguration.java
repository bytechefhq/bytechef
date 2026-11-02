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

package com.bytechef.automation.task.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.automation.task.service.ApprovalTaskServiceImpl;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@Configuration
public class AutomationTaskIntTestConfiguration {

    private static final String SIGNING_SECRET = "approval-token-secret-key-0001!!";

    @Bean
    ApprovalTaskService approvalTaskService(ApprovalTaskRepository approvalTaskRepository) {
        return new ApprovalTaskServiceImpl(approvalTaskRepository);
    }

    @Bean
    ApprovalTokens approvalTokens() {
        byte[] secret = SIGNING_SECRET.getBytes(StandardCharsets.UTF_8);

        return new ApprovalTokensImpl(
            Clock.systemUTC(), EncodingUtils.base64EncodeToString(secret), List.of(), Duration.ofDays(1),
            Duration.ofMinutes(1), false);
    }

    @Bean
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
    }

    @Bean
    TenantService tenantService() {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID));

        return tenantService;
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationTaskIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI")
        public AutomationTaskIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new ExecutionErrorToStringConverter(objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToExecutionErrorConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper),
                new StringToWorkflowTaskConverter(objectMapper),
                new WebhooksToStringConverter(objectMapper),
                new WorkflowTaskToStringConverter(objectMapper));
        }
    }
}
