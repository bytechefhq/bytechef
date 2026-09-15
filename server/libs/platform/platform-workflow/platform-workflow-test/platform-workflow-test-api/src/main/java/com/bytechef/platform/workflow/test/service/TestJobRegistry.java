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

import java.util.Optional;

/**
 * Remembers which workflow and environment each editor test run belongs to. Test jobs live in an in-memory job store
 * that deletes them when the run ends, so this is what a permission check on a test job id reads — while the run is
 * attached, when it is stopped, and when its editor logs are read afterwards.
 *
 * @author Ivica Cardic
 */
public interface TestJobRegistry {

    Optional<TestJob> fetchTestJob(long jobId);

    void register(long jobId, String workflowId, long environmentId);

    record TestJob(String workflowId, long environmentId) {
    }
}
