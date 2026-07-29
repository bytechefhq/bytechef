/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Boots the tool_invocation_log repository/service against a real PostgreSQL container. The EE listener and retention
 * job are excluded — they need the {@code ee} edition and scheduling, which are out of scope for the persistence test.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.platform.tool.invocation.log",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = {
            "com\\.bytechef\\.ee\\.platform\\.tool\\.invocation\\.log\\.ToolInvocationLogEventListener",
            "com\\.bytechef\\.ee\\.platform\\.tool\\.invocation\\.log\\.ToolInvocationLogRetentionJob"
        }))
@Import({
    LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@EnableAutoConfiguration
@Configuration
public class ToolInvocationLogIntTestConfiguration {

    // The nested class must carry a configuration-candidate annotation of its own: @TestConfiguration on
    // AbstractIntTestJdbcConfiguration is not @Inherited, and this subclass declares no @Bean method, so without
    // @Configuration Spring never registers it. No AbstractJdbcConfiguration bean means
    // ToolInvocationLogJdbcRepositoryConfiguration (@ConditionalOnBean) backs off and the repository is missing.
    @Configuration
    public static class ToolInvocationLogIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        @Override
        protected List<?> userConverters() {
            return List.of();
        }
    }
}
