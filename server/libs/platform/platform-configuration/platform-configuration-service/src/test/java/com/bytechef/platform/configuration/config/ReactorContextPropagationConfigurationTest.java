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

package com.bytechef.platform.configuration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Ivica Cardic
 */
class ReactorContextPropagationConfigurationTest {

    @BeforeEach
    void setUp() {
        new ReactorContextPropagationConfiguration();
    }

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testEnvironmentPropagatesAcrossBoundedElasticHop() {
        EnvironmentContext.set(Environment.DEVELOPMENT);

        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicReference<Environment> onScheduler = new AtomicReference<>();

        Mono.fromSupplier(() -> {
            threadName.set(Thread.currentThread()
                .getName());
            onScheduler.set(EnvironmentContext.getCurrentEnvironment());

            return true;
        })
            .subscribeOn(Schedulers.boundedElastic())
            .block();

        assertThat(threadName.get()).contains("boundedElastic");
        assertThat(onScheduler.get()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testTenantPropagatesAcrossBoundedElasticHop() {
        TenantContext.setCurrentTenantId("acme");

        AtomicReference<String> onScheduler = new AtomicReference<>();

        Mono.fromSupplier(() -> {
            onScheduler.set(TenantContext.getCurrentTenantId());

            return true;
        })
            .subscribeOn(Schedulers.boundedElastic())
            .block();

        assertThat(onScheduler.get()).isEqualTo("acme");
    }
}
