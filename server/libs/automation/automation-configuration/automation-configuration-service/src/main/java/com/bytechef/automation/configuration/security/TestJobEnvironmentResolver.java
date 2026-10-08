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

package com.bytechef.automation.configuration.security;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.test.service.TestJobRegistry;
import com.bytechef.platform.workflow.test.service.TestJobRegistry.TestJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Reports the environment an editor test job ran in, so attaching to it, stopping it and reading its logs are checked
 * against the same environment that starting it was.
 *
 * @author Ivica Cardic
 */
@Component
public class TestJobEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ObjectProvider<TestJobRegistry> testJobRegistryProvider;

    @SuppressFBWarnings("EI")
    public TestJobEnvironmentResolver(ObjectProvider<TestJobRegistry> testJobRegistryProvider) {
        this.testJobRegistryProvider = testJobRegistryProvider;
    }

    @Override
    public String resourceType() {
        return "TestJob";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        TestJobRegistry testJobRegistry = testJobRegistryProvider.getIfAvailable();

        if (!(id instanceof Number number) || testJobRegistry == null) {
            return Optional.empty();
        }

        Optional<TestJob> testJob = testJobRegistry.fetchTestJob(number.longValue());

        if (testJob.isEmpty()) {
            return Optional.empty();
        }

        long environmentId = testJob.get()
            .environmentId();

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            throw new IllegalStateException(
                "Test job " + number + " was registered with unknown environment ordinal " + environmentId);
        }

        return Optional.of(environments[(int) environmentId]);
    }
}
