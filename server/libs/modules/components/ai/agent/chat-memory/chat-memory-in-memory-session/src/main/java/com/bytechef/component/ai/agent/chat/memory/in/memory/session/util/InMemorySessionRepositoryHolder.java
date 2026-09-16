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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("MS")
public final class InMemorySessionRepositoryHolder {

    private static final Map<String, SessionRepository> REPOSITORIES = new ConcurrentHashMap<>();

    private InMemorySessionRepositoryHolder() {
    }

    public static SessionRepository getInstance() {
        return REPOSITORIES.computeIfAbsent(
            TenantContext.getCurrentTenantId(),
            tenantId -> InMemorySessionRepository.builder()
                .build());
    }
}
