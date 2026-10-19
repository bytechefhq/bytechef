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

package com.bytechef.platform.coordinator.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @author Ivica Cardic
 */
class NotificationThrottleTest {

    private final ConcurrentMapCacheManager concurrentMapCacheManager = createCacheManager();
    private final Cache claimCache = concurrentMapCacheManager.getCache(NotificationThrottle.CLAIM_CACHE_NAME);
    private final Cache suppressedCountCache = concurrentMapCacheManager.getCache(
        NotificationThrottle.SUPPRESSED_COUNT_CACHE_NAME);
    private final NotificationThrottle notificationThrottle = new NotificationThrottle(
        claimCache, suppressedCountCache, Duration.ofHours(1));

    @Test
    void testFirstNotificationIsSentWithNoSuppressedCount() {
        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
    }

    @Test
    void testRepeatedNotificationWhileClaimedIsSuppressed() {
        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .isEmpty();
    }

    @Test
    void testNotificationAfterTheClaimExpiresIsSentWithTheSuppressedCount() {
        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        for (int i = 0; i < 3; i++) {
            notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);
        }

        claimCache.clear();

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(3);

        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        claimCache.clear();

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(1);
    }

    @Test
    void testDifferentNotificationWorkflowOrEventTypeIsThrottledSeparately() {
        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        assertThat(notificationThrottle.tryAcquire(2L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
        assertThat(notificationThrottle.tryAcquire(1L, "workflow2", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_COMPLETED))
            .hasValue(0);
    }

    @Test
    void testDifferentTenantsAreThrottledSeparately() {
        notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        AtomicReference<OptionalLong> suppressedCount = new AtomicReference<>();

        TenantContext.runWithTenantId(
            "tenant2",
            () -> suppressedCount.set(
                notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED)));

        assertThat(suppressedCount.get()).hasValue(0);
    }

    @Test
    void testOnlyOneOfTheInstancesSharingTheCachesSendsPerWindow() {
        NotificationThrottle otherNotificationThrottle = new NotificationThrottle(
            claimCache, suppressedCountCache, Duration.ofHours(1));

        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
        assertThat(otherNotificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .isEmpty();
        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .isEmpty();

        claimCache.clear();

        assertThat(otherNotificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(2);
        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .isEmpty();
    }

    @Test
    void testZeroWindowDisablesThrottlingWithoutTouchingTheCaches() {
        NotificationThrottle disabledNotificationThrottle = new NotificationThrottle(
            claimCache, suppressedCountCache, Duration.ZERO);

        disabledNotificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED);

        assertThat(disabledNotificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
        assertThat(notificationThrottle.tryAcquire(1L, "workflow1", NotificationEvent.Type.JOB_FAILED))
            .hasValue(0);
    }

    private static ConcurrentMapCacheManager createCacheManager() {
        ConcurrentMapCacheManager concurrentMapCacheManager = new ConcurrentMapCacheManager();

        concurrentMapCacheManager.setStoreByValue(true);

        return concurrentMapCacheManager;
    }
}
