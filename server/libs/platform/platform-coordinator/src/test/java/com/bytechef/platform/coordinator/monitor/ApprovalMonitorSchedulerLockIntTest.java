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

package com.bytechef.platform.coordinator.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class ApprovalMonitorSchedulerLockIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS shedlock (name VARCHAR(64) NOT NULL PRIMARY KEY, " +
                "lock_until TIMESTAMP NOT NULL, locked_at TIMESTAMP NOT NULL, locked_by VARCHAR(255) NOT NULL)");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM shedlock");
    }

    @Test
    void testEachApprovalSweepRunsOnOneCoordinatorWhenEveryCoordinatorFiresIt() throws Exception {
        AtomicInteger expirySweeps = new AtomicInteger();
        AtomicInteger notificationSweeps = new AtomicInteger();

        try (AnnotationConfigApplicationContext firstCoordinator = createCoordinator(
            expirySweeps, notificationSweeps);
            AnnotationConfigApplicationContext secondCoordinator = createCoordinator(
                expirySweeps, notificationSweeps)) {

            fireConcurrently(firstCoordinator, secondCoordinator);

            assertThat(expirySweeps).hasValue(1);
            assertThat(notificationSweeps).hasValue(1);

            fireConcurrently(secondCoordinator, firstCoordinator);

            assertThat(expirySweeps).hasValue(1);
            assertThat(notificationSweeps).hasValue(1);
        }

        assertThat(jdbcTemplate.queryForList("SELECT name FROM shedlock ORDER BY name", String.class))
            .containsExactly("approvalExpiryMonitor", "approvalNotificationMonitor");
    }

    private AnnotationConfigApplicationContext createCoordinator(
        AtomicInteger expirySweeps, AtomicInteger notificationSweeps) {

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.registerBean(DataSource.class, () -> new DelegatingDataSource(dataSource));
        applicationContext.registerBean(
            ApprovalExpiryMonitor.class, () -> createApprovalExpiryMonitor(countingTenantService(expirySweeps)));
        applicationContext.registerBean(
            ApprovalNotificationMonitor.class,
            () -> createApprovalNotificationMonitor(countingTenantService(notificationSweeps)));
        applicationContext.register(SchedulerLockTestConfiguration.class);

        applicationContext.refresh();

        return applicationContext;
    }

    @SuppressWarnings("unchecked")
    private static ApprovalExpiryMonitor createApprovalExpiryMonitor(TenantService tenantService) {
        return new ApprovalExpiryMonitor(
            mock(ApplicationEventPublisher.class), mock(JobService.class), mock(ObjectProvider.class),
            mock(TaskExecutionService.class), null, tenantService);
    }

    private static ApprovalNotificationMonitor createApprovalNotificationMonitor(TenantService tenantService) {
        return new ApprovalNotificationMonitor(
            null, Duration.ofHours(4), mock(JobService.class), new NotificationHandlerRegistry(List.of()),
            new NotificationSenderRegistry(List.of()), mock(NotificationService.class), null, Duration.ofHours(24),
            mock(TaskExecutionService.class), tenantService);
    }

    private static TenantService countingTenantService(AtomicInteger sweeps) {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenAnswer(invocation -> {
            sweeps.incrementAndGet();

            return List.of();
        });

        return tenantService;
    }

    private static void fireConcurrently(
        AnnotationConfigApplicationContext firstCoordinator, AnnotationConfigApplicationContext secondCoordinator)
        throws Exception {

        CyclicBarrier cyclicBarrier = new CyclicBarrier(4);

        ApprovalExpiryMonitor firstApprovalExpiryMonitor = firstCoordinator.getBean(ApprovalExpiryMonitor.class);
        ApprovalExpiryMonitor secondApprovalExpiryMonitor = secondCoordinator.getBean(ApprovalExpiryMonitor.class);
        ApprovalNotificationMonitor firstApprovalNotificationMonitor = firstCoordinator.getBean(
            ApprovalNotificationMonitor.class);
        ApprovalNotificationMonitor secondApprovalNotificationMonitor = secondCoordinator.getBean(
            ApprovalNotificationMonitor.class);

        List<CompletableFuture<Void>> firings = List.of(
            fire(firstApprovalExpiryMonitor::sweepExpiredApprovals, cyclicBarrier),
            fire(secondApprovalExpiryMonitor::sweepExpiredApprovals, cyclicBarrier),
            fire(firstApprovalNotificationMonitor::notifyPendingApprovals, cyclicBarrier),
            fire(secondApprovalNotificationMonitor::notifyPendingApprovals, cyclicBarrier));

        CompletableFuture<Void> allFirings = CompletableFuture.allOf(firings.toArray(CompletableFuture[]::new));

        allFirings.get();
    }

    private static CompletableFuture<Void> fire(Runnable sweep, CyclicBarrier cyclicBarrier) {
        return CompletableFuture.runAsync(() -> {
            try {
                cyclicBarrier.await();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }

            sweep.run();
        });
    }

    @Configuration
    @EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
    static class SchedulerLockTestConfiguration {

        @Bean
        LockProvider lockProvider(DataSource dataSource) {
            return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                    .withJdbcTemplate(new JdbcTemplate(dataSource))
                    .usingDbTime()
                    .build());
        }
    }
}
