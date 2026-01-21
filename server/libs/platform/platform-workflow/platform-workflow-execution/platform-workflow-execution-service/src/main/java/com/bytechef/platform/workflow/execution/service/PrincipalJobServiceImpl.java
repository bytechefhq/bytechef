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

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
public class PrincipalJobServiceImpl implements PrincipalJobService {

    private final PrincipalJobRepository principalJobRepository;

    @SuppressFBWarnings("EI")
    public PrincipalJobServiceImpl(PrincipalJobRepository principalJobRepository) {
        this.principalJobRepository = principalJobRepository;
    }

    @Override
    public PrincipalJob create(long jobId, long principalId, PlatformType type) {
        return principalJobRepository.save(new PrincipalJob(principalId, jobId, type));
    }

    @Override
    public void deletePrincipalJobs(long jobId, PlatformType type) {
        principalJobRepository.findByJobIdAndType(jobId, type.ordinal())
            .ifPresent(instanceJob -> principalJobRepository.deleteById(Validate.notNull(instanceJob.getId(), "id")));
    }

    @Override
    public Optional<Long> fetchLastJobId(long principalId, PlatformType type) {
        return principalJobRepository
            .findTop1ByPrincipalIdAndTypeOrderByJobIdDesc(principalId, type.ordinal())
            .map(PrincipalJob::getJobId);
    }

    @Override
    public Optional<Long> fetchLastWorkflowJobId(long principalId, List<String> workflowIds, PlatformType type) {
        Page<Long> page = principalJobRepository.findAllJobIds(
            null, null, null, List.of(principalId), type.ordinal(), workflowIds, PageRequest.of(0, 1));

        return page.getContent()
            .stream()
            .findFirst();
    }

    @Override
    public Optional<Long> fetchJobPrincipalId(long jobId, PlatformType type) {
        return principalJobRepository.findByJobIdAndType(jobId, type.ordinal())
            .map(PrincipalJob::getPrincipalId);
    }

    @Override
    public long getJobPrincipalId(long jobId, PlatformType type) {
        return principalJobRepository.findByJobIdAndType(jobId, type.ordinal())
            .map(PrincipalJob::getPrincipalId)
            .orElseThrow();
    }

    @Override
    public List<Long> getJobIds(long principalId, PlatformType type) {
        return principalJobRepository.findallJobIds(principalId, type.ordinal());
    }

    @Override
    public Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, PlatformType type,
        List<String> workflowIds, int pageNumber) {

        PageRequest pageRequest = PageRequest.of(pageNumber, PrincipalJobRepository.DEFAULT_PAGE_SIZE);

        return principalJobRepository.findAllJobIds(
            status == null ? null : status.ordinal(), startDate, endDate, principalIds, type.ordinal(), workflowIds,
            pageRequest);
    }
}
