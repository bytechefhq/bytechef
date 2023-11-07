/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.execution.service;

import com.bytechef.hermes.execution.domain.InstanceJob;
import com.bytechef.hermes.execution.repository.InstanceJobRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class InstanceJobServiceImpl implements InstanceJobService {

    private final InstanceJobRepository instanceJobRepository;

    @SuppressFBWarnings("EI")
    public InstanceJobServiceImpl(InstanceJobRepository instanceJobRepository) {
        this.instanceJobRepository = instanceJobRepository;
    }

    @Override
    public InstanceJob create(long jobId, long instanceId, int type) {
        return instanceJobRepository.save(new InstanceJob(instanceId, jobId, type));
    }

    @Override
    public Optional<Long> fetchJobInstanceId(long jobId, int type) {
        return instanceJobRepository.findByJobIdAndType(jobId, type)
            .map(InstanceJob::getInstanceId);
    }
}
