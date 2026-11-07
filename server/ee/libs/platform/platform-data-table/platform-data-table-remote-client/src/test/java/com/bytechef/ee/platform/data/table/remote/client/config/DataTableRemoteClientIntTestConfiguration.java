/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.config;

import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.OptionalLong;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.data.table")
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@Configuration
public class DataTableRemoteClientIntTestConfiguration {

    @Bean
    DataTableWorkspaceResolver dataTableWorkspaceResolver() {
        return new DataTableWorkspaceResolver() {

            @Override
            public OptionalLong resolveByWorkflowId(String workflowId) {
                return workflowId.equals("workflow-1") ? OptionalLong.of(7L) : OptionalLong.empty();
            }

            @Override
            public OptionalLong resolveByJobPrincipalId(long jobPrincipalId, PlatformType platformType) {
                return jobPrincipalId == 3L && platformType == PlatformType.AUTOMATION
                    ? OptionalLong.of(8L) : OptionalLong.empty();
            }

            @Override
            public OptionalLong resolveByWorkflowUuid(String workflowUuid) {
                return workflowUuid.equals("workflow-uuid") ? OptionalLong.of(9L) : OptionalLong.empty();
            }
        };
    }

    @Bean
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry() {
        return new JobPrincipalAccessorRegistry(List.of());
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    TagService tagService() {
        return mock(TagService.class);
    }

    @Bean
    WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade() {
        return mock(WorkflowNodeTestOutputFacade.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class DataTableRemoteClientIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
