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

package com.bytechef.component.ai.agent.chat.memory.in.memory.session.util;

import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;

/**
 * Tenant-scoped holder for {@link InMemorySessionRepository} instances.
 *
 * <p>
 * Each tenant gets its own repository so session IDs cannot collide across tenants. The repositories live in a Caffeine
 * cache rather than a plain {@link java.util.concurrent.ConcurrentHashMap} so idle tenants don't pin memory forever —
 * {@code expireAfterAccess} keeps an actively chatting tenant hot while dropping tenants that have gone silent for the
 * TTL window. This data is intentionally process-local, so we use Caffeine directly rather than the Spring
 * {@code CacheManager} abstraction that would otherwise allow swapping in Redis.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("MS")
public final class InMemorySessionRepositoryHolder {

    private static final Cache<String, SessionRepository> REPOSITORIES = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    private InMemorySessionRepositoryHolder() {
    }

    public static SessionRepository getInstance() {
        return REPOSITORIES.get(
            TenantContext.getCurrentTenantId(),
            _ -> InMemorySessionRepository.builder()
                .build());
    }
}
