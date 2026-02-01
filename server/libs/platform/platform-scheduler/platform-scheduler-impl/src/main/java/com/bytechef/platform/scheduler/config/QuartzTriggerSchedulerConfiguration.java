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

package com.bytechef.platform.scheduler.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.QuartzTriggerScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.quartz.autoconfigure.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(QuartzProperties.class)
@ConditionalOnProperty(
    prefix = "bytechef", name = "coordinator.trigger.scheduler.provider", havingValue = "quartz", matchIfMissing = true)
public class QuartzTriggerSchedulerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(QuartzTriggerSchedulerConfiguration.class);

    @Bean
    JobFactory jobFactory() {
        return new AutowiringSpringBeanJobFactory();
    }

    @Bean
    TriggerScheduler quartzTriggerScheduler(ApplicationProperties applicationProperties, @Lazy Scheduler scheduler) {
        ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties.getCoordinator()
            .getTrigger()
            .getPolling();

        return new QuartzTriggerScheduler(polling, scheduler);
    }

    @Bean
    QuartzDelayer quartzDelayer(@Lazy Scheduler scheduler) {
        return new QuartzDelayer(scheduler);
    }

    private static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory
        implements ApplicationContextAware {

        private transient AutowireCapableBeanFactory beanFactory;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            beanFactory = applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            final Object job = super.createJobInstance(bundle);

            beanFactory.autowireBean(job);

            return job;
        }
    }

    private record QuartzDelayer(Scheduler scheduler) {

        @EventListener(ApplicationReadyEvent.class)
        public void startLater() throws SchedulerException {
            scheduler.start();

            if (logger.isDebugEnabled()) {
                logger.debug("Quartz scheduler started");
            }
        }
    }
}
