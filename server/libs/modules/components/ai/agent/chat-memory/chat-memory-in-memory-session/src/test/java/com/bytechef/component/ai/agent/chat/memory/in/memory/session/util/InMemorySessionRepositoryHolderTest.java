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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
class InMemorySessionRepositoryHolderTest {

    @Test
    void testSameTenantGetsTheSameRepository() {
        SessionRepository firstSessionRepository = TenantContext.callWithTenantId(
            "000001", InMemorySessionRepositoryHolder::getInstance);
        SessionRepository secondSessionRepository = TenantContext.callWithTenantId(
            "000001", InMemorySessionRepositoryHolder::getInstance);

        assertThat(secondSessionRepository).isSameAs(firstSessionRepository);
    }

    @Test
    void testTenantsGetDifferentRepositories() {
        SessionRepository firstTenantSessionRepository = TenantContext.callWithTenantId(
            "000001", InMemorySessionRepositoryHolder::getInstance);
        SessionRepository secondTenantSessionRepository = TenantContext.callWithTenantId(
            "000002", InMemorySessionRepositoryHolder::getInstance);

        assertThat(secondTenantSessionRepository).isNotSameAs(firstTenantSessionRepository);
    }
}
