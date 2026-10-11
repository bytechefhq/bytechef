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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.platform.workflow.execution.JobResumeId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public final class InMemoryTaskStateService implements TaskStateService {

    private final Map<JobResumeId, Object> store = new ConcurrentHashMap<>();

    @Override
    public void delete(JobResumeId jobResumeId) {
        store.remove(jobResumeId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
        return Optional.ofNullable((T) store.get(jobResumeId));
    }

    @Override
    public void save(JobResumeId jobResumeId, Object value) {
        store.put(jobResumeId, value);
    }
}
