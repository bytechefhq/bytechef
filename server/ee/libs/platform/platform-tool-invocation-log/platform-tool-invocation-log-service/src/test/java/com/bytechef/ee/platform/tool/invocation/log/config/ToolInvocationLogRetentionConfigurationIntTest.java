/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogRetentionJob;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolInvocationLogRetentionConfigurationIntTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ToolInvocationLogRetentionConfiguration.class))
        .withPropertyValues("bytechef.edition=ee")
        .withBean(ApplicationProperties.class, ApplicationProperties::new)
        .withBean(ToolInvocationLogService.class, () -> mock(ToolInvocationLogService.class));

    @Test
    void testRetentionJobRunsInAnApplicationThatManagesTenants() {
        applicationContextRunner
            .withBean(TenantService.class, () -> mock(TenantService.class))
            .run(context -> assertThat(context).hasSingleBean(ToolInvocationLogRetentionJob.class));
    }

    @Test
    void testRetentionJobIsLeftOutOfAnApplicationWithoutTenants() {
        applicationContextRunner
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ToolInvocationLogRetentionJob.class);
            });
    }
}
