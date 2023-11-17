/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.execution.repository.jdbc;

import static java.time.temporal.ChronoUnit.DAYS;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.jdbc.config.WorkflowExecutionRepositoryIntTestConfiguration;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowExecutionRepositoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcJobRepositoryIntTest {

    @Autowired
    private JdbcJobRepository jobRepository;

    @AfterEach
    public void afterEach() {
        jobRepository.deleteAll();
    }

    @Test
    public void test1FindAll() {
        int pageTotal = jobRepository
            .findAll(PageRequest.of(0, JobRepository.DEFAULT_PAGE_SIZE))
            .getNumberOfElements();

        Job job = jobRepository.save(getJob(Job.Status.STARTED));

        Page<Job> page = jobRepository.findAll(PageRequest.of(0, JobRepository.DEFAULT_PAGE_SIZE));

        Assertions.assertEquals(pageTotal + 1, page.getNumberOfElements());

        Job one = OptionalUtils.get(jobRepository.findById(Validate.notNull(job.getId(), "id")));

        Assertions.assertNotNull(one);
    }

    @Test
    public void testFindById() {
        Job job = jobRepository.save(getJob(Job.Status.CREATED));

        Job resultJob = OptionalUtils.get(jobRepository.findById(Validate.notNull(job.getId(), "id")));

        resultJob.setId(null);
        resultJob.setStatus(Job.Status.FAILED);

        // test immutability
        Assertions.assertNotEquals(job.getStatus(), resultJob.getStatus());

        job = jobRepository.save(resultJob);

        resultJob = OptionalUtils.get(jobRepository.findById(Validate.notNull(job.getId(), "id")));

        Assertions.assertEquals(Job.Status.FAILED, resultJob.getStatus());
    }

    @Test
    public void testCountCompletedJobsToday() {
        int countCompletedJobsToday = jobRepository.countCompletedJobsToday();

        Job job = jobRepository.save(getJob(Job.Status.CREATED));

        Instant now = Instant.now();

        job.setEndDate(LocalDateTime.ofInstant(now.minus(1, DAYS), ZoneId.systemDefault()));

        jobRepository.save(job);

        for (int i = 0; i < 5; i++) {
            Job completedJobToday = jobRepository.save(getJob(Job.Status.COMPLETED));

            completedJobToday.setEndDate(LocalDateTime.now());

            jobRepository.save(completedJobToday);
        }

        Job runningJobToday = new Job();

        runningJobToday.setStatus(Job.Status.STARTED);
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
            Job completedJobYesterday = jobRepository.save(getJob(Job.Status.COMPLETED));

            Instant now = Instant.now();

            completedJobYesterday.setEndDate(LocalDateTime.ofInstant(now.minus(1, DAYS), ZoneId.systemDefault()));

            jobRepository.save(completedJobYesterday);
        }

        Job runningJobYesterday = jobRepository.save(getJob(Job.Status.STARTED));

        jobRepository.save(runningJobYesterday);

        Job completedJobToday = new Job();

        completedJobToday.setId(null);
        completedJobToday.setStatus(Job.Status.COMPLETED);
        completedJobToday.setWorkflowId("demo:1234");

        completedJobToday = jobRepository.save(completedJobToday);

        completedJobToday.setEndDate(LocalDateTime.now());
        completedJobToday.setId(null);

        jobRepository.save(completedJobToday);

        // act
        int yesterdayJobs = jobRepository.countCompletedJobsYesterday();

        // assert
        Assertions.assertEquals(countCompletedJobsYesterday + 5, yesterdayJobs);
    }

    private static Job getJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        return job;
    }
}
