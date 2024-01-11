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
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public InstanceJob create(long jobId, long instanceId, PlatformType type) {
        return instanceJobRepository.save(new InstanceJob(instanceId, jobId, type.getId()));
    }

    @Override
    public Optional<Long> fetchLastJobId(long instanceId, PlatformType type) {
        return instanceJobRepository.findTop1ByInstanceIdAndTypeOrderByJobIdDesc(instanceId, type.getId())
            .map(InstanceJob::getJobId);
    }

    @Override
    public Optional<Long> fetchJobInstanceId(long jobId, PlatformType type) {
        return instanceJobRepository.findByJobIdAndType(jobId, type.getId())
            .map(InstanceJob::getInstanceId);
    }

    @Override
    public Page<Long> getJobIds(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, PlatformType type,
        List<String> workflowIds, int pageNumber) {

        PageRequest pageRequest = PageRequest.of(pageNumber, InstanceJobRepository.DEFAULT_PAGE_SIZE);

        return instanceJobRepository.findAllJobIds(
            status, startDate, endDate, instanceId, type.getId(), workflowIds, pageRequest);
    }
}
