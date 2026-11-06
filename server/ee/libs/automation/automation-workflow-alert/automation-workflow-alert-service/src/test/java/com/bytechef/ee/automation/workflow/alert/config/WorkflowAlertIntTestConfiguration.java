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
 * @version ee
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.automation.workflow.alert",
    excludeFilters = @Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            WorkflowAlertApplicationEventListener.class, WorkflowAlertDispatcher.class,
            WorkflowAlertNoActivityMonitor.class
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
