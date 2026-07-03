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

package com.bytechef.platform.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Integration test for {@link QuartzAgentScheduler}. Uses an in-memory Quartz scheduler with Spring autowiring of job
 * beans so that {@code AgentScheduleJob} can publish application events.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = QuartzAgentSchedulerIntTest.AgentSchedulerTestBootstrap.class,
    properties = "spring.profiles.active=test")
@Import(QuartzAgentSchedulerIntTest.QuartzAgentSchedulerIntTestConfiguration.class)
class QuartzAgentSchedulerIntTest {

    @Autowired
    private AgentScheduler agentScheduler;

    @Autowired
    private FireCounter fireCounter;

    @AfterEach
    void cleanup() {
        agentScheduler.cancelAgentRun(42L);
        fireCounter.reset();
    }

    @Test
    void testScheduleAgentRunFiresOnCron() {
        agentScheduler.scheduleAgentRun(42L, "0/2 * * * * ?", "UTC", null);

        await().atMost(Duration.ofSeconds(8))
            .until(() -> fireCounter.count() >= 1);

        assertThat(fireCounter.count()).isGreaterThanOrEqualTo(1);
        assertThat(fireCounter.lastScheduleId()).isEqualTo(42L);
    }

    @Test
    void testCancelAgentRunRemovesTrigger() {
        agentScheduler.scheduleAgentRun(42L, "0/1 * * * * ?", "UTC", null);
        agentScheduler.cancelAgentRun(42L);

        assertThat(agentScheduler.exists(42L)).isFalse();
    }

    @SpringBootConfiguration
    static class AgentSchedulerTestBootstrap {
    }

    @TestConfiguration
    static class QuartzAgentSchedulerIntTestConfiguration {

        @Bean
        @Primary
        Scheduler scheduler(ApplicationContext applicationContext) throws SchedulerException {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();

            Scheduler quartzScheduler = schedulerFactory.getScheduler();

            SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();

            jobFactory.setApplicationContext(applicationContext);

            quartzScheduler.setJobFactory(jobFactory);
            quartzScheduler.start();

            return quartzScheduler;
        }

        @Bean
        @Primary
        AgentScheduler agentScheduler(Scheduler scheduler) {
            return new QuartzAgentScheduler(scheduler);
        }

        @Bean
        FireCounter fireCounter() {
            return new FireCounter();
        }
    }

    static class FireCounter {

        private final AtomicInteger count = new AtomicInteger();
        private volatile long lastScheduleId = -1;

        @EventListener
        void onFired(AgentScheduleFiredEvent event) {
            count.incrementAndGet();
            lastScheduleId = event.agentScheduleId();
        }

        int count() {
            return count.get();
        }

        long lastScheduleId() {
            return lastScheduleId;
        }

        void reset() {
            count.set(0);
            lastScheduleId = -1;
        }
    }
}
