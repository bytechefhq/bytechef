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

package com.bytechef.platform.workflow.test.service;

import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Keyed per tenant, and kept for a day after the last read so editor logs stay readable after the run ends. The
 * registry is local to the node that ran the test.
 *
 * @author Ivica Cardic
 */
public class TestJobRegistryImpl implements TestJobRegistry {

    private final Cache<String, TestJob> testJobs = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS)
        .maximumSize(10_000)
        .build();

    @Override
    public Optional<TestJob> fetchTestJob(long jobId) {
        return Optional.ofNullable(testJobs.getIfPresent(TenantCacheKeyUtils.getKey(jobId)));
    }

    @Override
    public void register(long jobId, String workflowId, long environmentId) {
        testJobs.put(TenantCacheKeyUtils.getKey(jobId), new TestJob(workflowId, environmentId));
    }
}
