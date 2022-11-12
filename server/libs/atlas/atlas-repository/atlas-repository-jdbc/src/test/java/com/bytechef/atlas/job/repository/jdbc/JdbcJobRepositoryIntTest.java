/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.job.repository.jdbc;

import static java.time.temporal.ChronoUnit.DAYS;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.repository.jdbc.config.WorkflowRepositoryIntTestConfiguration;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.test.extension.PostgresTestContainerExtension;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@ExtendWith(PostgresTestContainerExtension.class)
@SpringBootTest(
        classes = WorkflowRepositoryIntTestConfiguration.class,
        properties = "bytechef.workflow.persistence.provider=jdbc")
public class JdbcJobRepositoryIntTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    public void test1FindAll() {
        int pageTotal = jobRepository
                .findAll(PageRequest.of(0, JobRepository.DEFAULT_PAGE_SIZE))
                .getNumberOfElements();

        Job job = jobRepository.save(getJob(JobStatus.STARTED));

        Page<Job> page = jobRepository.findAll(PageRequest.of(0, JobRepository.DEFAULT_PAGE_SIZE));

        Assertions.assertEquals(pageTotal + 1, page.getNumberOfElements());

        Job one = jobRepository.findById(job.getId()).orElseThrow();

        Assertions.assertNotNull(one);
    }

    @Test
    public void testFindById() {
        Job job = jobRepository.save(getJob(JobStatus.CREATED));

        Job resultJob = jobRepository.findById(job.getId()).orElseThrow();

        job = new Job(resultJob);

        job.setId(null);
        job.setNew(true);
        job.setStatus(JobStatus.FAILED);

        // test immutability
        Assertions.assertNotEquals(job.getStatus(), resultJob.getStatus());

        job = jobRepository.save(job);

        resultJob = jobRepository.findById(job.getId()).orElseThrow();

        Assertions.assertEquals("FAILED", resultJob.getStatus().toString());
    }

    @Test
    public void testCountCompletedJobsToday() {
        int countCompletedJobsToday = jobRepository.countCompletedJobsToday();

        Job job = jobRepository.save(getJob(JobStatus.CREATED));

        job.setCreatedDate(LocalDateTime.now().minus(2, DAYS));
        job.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
        job.setNew(false);

        jobRepository.save(job);

        for (int i = 0; i < 5; i++) {
            Job completedJobToday = jobRepository.save(getJob(JobStatus.COMPLETED));

            completedJobToday.setCreatedDate(LocalDateTime.now().minus(1, DAYS));
            completedJobToday.setEndTime(new Date());
            completedJobToday.setNew(false);

            jobRepository.save(completedJobToday);
        }

        Job runningJobToday = new Job();

        runningJobToday.setCreatedDate(LocalDateTime.now());
        runningJobToday.setStatus(JobStatus.STARTED);
        runningJobToday.setNew(true);
        runningJobToday.setWorkflowId("demo:1234");

        jobRepository.save(runningJobToday);

        // act
        int todayJobs = jobRepository.countCompletedJobsToday();

        // assert
        Assertions.assertEquals(countCompletedJobsToday + 5, todayJobs);
    }

    @Test
    public void testCountCompletedJobsYesterday() {
        int countCompletedJobsYesterday = jobRepository.countCompletedJobsYesterday();

        for (int i = 0; i < 5; i++) {
            Job completedJobYesterday = jobRepository.save(getJob(JobStatus.COMPLETED));

            completedJobYesterday.setCreatedDate(LocalDateTime.now().minus(2, DAYS));
            completedJobYesterday.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
            completedJobYesterday.setNew(false);

            jobRepository.save(completedJobYesterday);
        }

        Job runningJobYesterday = jobRepository.save(getJob(JobStatus.STARTED));

        runningJobYesterday.setCreatedDate(LocalDateTime.now().minus(1, DAYS));
        runningJobYesterday.setNew(false);

        jobRepository.save(runningJobYesterday);

        Job completedJobToday = new Job();

        completedJobToday.setCreatedDate(LocalDateTime.now().minus(1, DAYS));
        completedJobToday.setNew(true);
        completedJobToday.setStatus(JobStatus.COMPLETED);
        completedJobToday.setWorkflowId("demo:1234");

        completedJobToday = jobRepository.save(completedJobToday);

        completedJobToday.setEndTime(new Date());
        completedJobToday.setNew(false);

        jobRepository.save(completedJobToday);

        // act
        int yesterdayJobs = jobRepository.countCompletedJobsYesterday();

        // assert
        Assertions.assertEquals(countCompletedJobsYesterday + 5, yesterdayJobs);
    }

    private static Job getJob(JobStatus status) {
        Job job = new Job();

        job.setNew(true);
        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        return job;
    }
}
