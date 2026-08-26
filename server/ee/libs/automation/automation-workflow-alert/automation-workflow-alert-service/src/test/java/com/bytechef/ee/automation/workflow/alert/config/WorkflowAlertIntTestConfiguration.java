/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.config;

import com.bytechef.ee.automation.workflow.alert.dispatcher.WorkflowAlertDispatcher;
import com.bytechef.ee.automation.workflow.alert.listener.WorkflowAlertApplicationEventListener;
import com.bytechef.ee.automation.workflow.alert.listener.WorkflowAlertNoActivityMonitor;
import com.bytechef.ee.automation.workflow.alert.listener.WorkflowAlertUsageMonitor;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * The module's first integration-test context.
 *
 * <p>
 * Scans the module but excludes the alert evaluator, the dispatcher and the two monitors: they pull in atlas,
 * notification, plan and execution-cost collaborators that a data-plane test has no use for, so including them would
 * mean mocking a dozen beans to exercise a repository query. What is left is the persistence slice. The
 * {@code WorkflowPreDeleteListener} is constructed directly by the test, the way the module's unit tests already do.
 * </p>
 *
 * <p>
 * The repositories are NOT enabled here. The production {@code WorkflowAlertJdbcRepositoryConfiguration} is an
 * auto-configuration gated on an {@code AbstractJdbcConfiguration} bean being present, which the nested JDBC
 * configuration below supplies — declaring {@code @EnableJdbcRepositories} again would register every repository twice
 * and fail the context with a bean-override error.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.automation.workflow.alert",
    excludeFilters = @Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            WorkflowAlertApplicationEventListener.class, WorkflowAlertDispatcher.class,
            WorkflowAlertNoActivityMonitor.class, WorkflowAlertUsageMonitor.class
        }))
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class WorkflowAlertIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class WorkflowAlertIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
