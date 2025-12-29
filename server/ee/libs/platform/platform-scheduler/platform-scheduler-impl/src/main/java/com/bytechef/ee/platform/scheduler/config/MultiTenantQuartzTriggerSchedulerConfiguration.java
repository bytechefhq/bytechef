/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.tenant.MultiTenantLocalDataSourceJobStore;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Properties;
import org.quartz.spi.JobFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.quartz.autoconfigure.QuartzProperties;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef", name = "coordinator.trigger.scheduler.provider", havingValue = "quartz", matchIfMissing = true)
@ConditionalOnEEVersion
public class MultiTenantQuartzTriggerSchedulerConfiguration implements SchedulerFactoryBeanCustomizer {

    private final JobFactory jobFactory;
    private final ApplicationProperties.Tenant.Mode mode;
    private final QuartzProperties quartzProperties;

    @SuppressFBWarnings("EI")
    public MultiTenantQuartzTriggerSchedulerConfiguration(
        ApplicationProperties applicationProperties, JobFactory jobFactory, QuartzProperties quartzProperties) {

        this.jobFactory = jobFactory;
        this.mode = applicationProperties.getTenant()
            .getMode();
        this.quartzProperties = quartzProperties;
    }

    @Override
    public void customize(SchedulerFactoryBean schedulerFactoryBean) {
        schedulerFactoryBean.setJobFactory(jobFactory);

        Properties properties = new Properties();

        properties.putAll(quartzProperties.getProperties());

        if (mode == ApplicationProperties.Tenant.Mode.MULTI) {
            properties.put("org.quartz.jobStore.class", MultiTenantLocalDataSourceJobStore.class.getName());
        }

        schedulerFactoryBean.setQuartzProperties(properties);
    }
}
