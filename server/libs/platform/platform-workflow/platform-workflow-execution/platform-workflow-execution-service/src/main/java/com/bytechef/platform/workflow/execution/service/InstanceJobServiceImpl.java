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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.workflow.execution.domain.InstanceJob;
import com.bytechef.platform.workflow.execution.repository.InstanceJobRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
    public InstanceJob create(long jobId, long instanceId, Type type) {
        return instanceJobRepository.save(new InstanceJob(instanceId, jobId, type));
    }

    @Override
    public void deleteInstanceJobs(long jobId, Type type) {
        instanceJobRepository.findByJobIdAndType(jobId, type.ordinal())
            .ifPresent(instanceJob -> instanceJobRepository.deleteById(Validate.notNull(instanceJob.getId(), "id")));
    }

    @Override
    public Optional<Long> fetchLastJobId(long instanceId, Type type) {
        return instanceJobRepository
            .findTop1ByInstanceIdAndTypeOrderByJobIdDesc(instanceId, type.ordinal())
            .map(InstanceJob::getJobId);
    }

    @Override
    public Optional<Long> fetchJobInstanceId(long jobId, Type type) {
        return instanceJobRepository.findByJobIdAndType(jobId, type.ordinal())
            .map(InstanceJob::getInstanceId);
    }

    @Override
    public long getJobInstanceId(long jobId, Type type) {
        return instanceJobRepository.findByJobIdAndType(jobId, type.ordinal())
            .map(InstanceJob::getInstanceId)
            .orElseThrow();
    }

    @Override
    public List<Long> getJobIds(long instanceId, Type type) {
        return instanceJobRepository.findallJobIds(instanceId, type.ordinal());
    }

    @Override
    public Page<Long> getJobIds(
        Status status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, Type type,
        List<String> workflowIds, int pageNumber) {

        PageRequest pageRequest = PageRequest.of(pageNumber, InstanceJobRepository.DEFAULT_PAGE_SIZE);

        return instanceJobRepository.findAllJobIds(
            status == null ? null : status.ordinal(), startDate, endDate, instanceId, type.ordinal(), workflowIds,
            pageRequest);
    }
}
