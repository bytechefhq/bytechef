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

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;

/**
 * @author Ivica Cardic
 */
public class NotificationThrottle {

    public static final String CLAIM_CACHE_NAME =
        "com.bytechef.platform.coordinator.notification.NotificationThrottle.claim";
    public static final String SUPPRESSED_COUNT_CACHE_NAME =
        "com.bytechef.platform.coordinator.notification.NotificationThrottle.suppressedCount";

    private final Cache claimCache;
    private final Cache suppressedCountCache;
    private final Duration window;

    @SuppressFBWarnings("EI")
    public NotificationThrottle(Cache claimCache, Cache suppressedCountCache, Duration window) {
        this.claimCache = claimCache;
        this.suppressedCountCache = suppressedCountCache;
        this.window = window;
    }

    public OptionalLong tryAcquire(Long notificationId, @Nullable String workflowId, NotificationEvent.Type eventType) {
        if (window.isZero() || window.isNegative()) {
            return OptionalLong.of(0);
        }

        String key = TenantCacheKeyUtils.getKey(notificationId, workflowId, eventType);

        Long suppressedCount = suppressedCountCache.get(key, Long.class);

        if (claimCache.putIfAbsent(key, Boolean.TRUE) == null) {
            suppressedCountCache.evict(key);

            return OptionalLong.of(suppressedCount == null ? 0 : suppressedCount);
        }

        suppressedCountCache.put(key, suppressedCount == null ? 1 : suppressedCount + 1);

        return OptionalLong.empty();
    }
}
