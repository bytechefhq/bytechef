/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.job;

import static java.time.temporal.ChronoUnit.DAYS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.integri.atlas.engine.coordinator.job.JdbcJobRepository;
import com.integri.atlas.engine.coordinator.Page;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.coordinator.task.JdbcTaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SpringBootTest
public class JdbcJobRepositoryTests {

    @Autowired
    private DataSource dataSource;

    @Test
    public void test1() {
        JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
        taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        taskRepository.setObjectMapper(createObjectMapper());

        JdbcJobRepository jobRepository = new JdbcJobRepository();
        jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        jobRepository.setJobTaskRepository(taskRepository);

        int pageTotal = jobRepository.getPage(1).getSize();

        String id = UUIDGenerator.generate();

        SimpleJob job = new SimpleJob();
        job.setWorkflowId("demo:1234");
        job.setId(id);
        job.setCreateTime(new Date());
        job.setStatus(JobStatus.CREATED);
        jobRepository.create(job);

        Page<JobSummary> all = jobRepository.getPage(1);
        Assertions.assertEquals(pageTotal + 1, all.getSize());

        Job one = jobRepository.getById(id);
        Assertions.assertNotNull(one);
    }

    @Test
    public void test2() {
        JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
        taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        taskRepository.setObjectMapper(createObjectMapper());

        JdbcJobRepository jobRepository = new JdbcJobRepository();
        jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        jobRepository.setJobTaskRepository(taskRepository);

        String id = UUIDGenerator.generate();

        SimpleJob job = new SimpleJob();
        job.setId(id);
        job.setWorkflowId("demo:1234");
        job.setCreateTime(new Date());
        job.setStatus(JobStatus.CREATED);
        jobRepository.create(job);

        Job one = jobRepository.getById(id);

        SimpleJob mjob = new SimpleJob(one);
        mjob.setStatus(JobStatus.FAILED);

        // test immutability
        Assertions.assertNotEquals(mjob.getStatus().toString(), one.getStatus().toString());

        jobRepository.merge(mjob);
        one = jobRepository.getById(id);
        Assertions.assertEquals("FAILED", one.getStatus().toString());
    }

    @Test
    public void test3() {
        // arrange
        JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
        taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        taskRepository.setObjectMapper(createObjectMapper());

        JdbcJobRepository jobRepository = new JdbcJobRepository();
        jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        jobRepository.setJobTaskRepository(taskRepository);

        String id = UUIDGenerator.generate();

        int countCompletedJobsToday = jobRepository.countCompletedJobsToday();

        SimpleJob completedJobYesterday = new SimpleJob();
        completedJobYesterday.setId(id);
        completedJobYesterday.setWorkflowId("demo:1234");
        completedJobYesterday.setCreateTime(Date.from(Instant.now().minus(2, DAYS)));
        completedJobYesterday.setStatus(JobStatus.COMPLETED);
        jobRepository.create(completedJobYesterday);
        completedJobYesterday.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
        jobRepository.merge(completedJobYesterday);

        for (int i = 0; i < 5; i++) {
            SimpleJob completedJobToday = new SimpleJob();
            completedJobToday.setId(UUIDGenerator.generate() + "." + i);
            completedJobToday.setWorkflowId("demo:1234");
            completedJobToday.setCreateTime(Date.from(Instant.now().minus(1, DAYS)));
            completedJobToday.setStatus(JobStatus.COMPLETED);
            jobRepository.create(completedJobToday);
            completedJobToday.setEndTime(new Date());
            jobRepository.merge(completedJobToday);
        }

        SimpleJob runningJobToday = new SimpleJob();
        runningJobToday.setId(UUIDGenerator.generate());
        runningJobToday.setWorkflowId("demo:1234");
        runningJobToday.setCreateTime(new Date());
        runningJobToday.setStatus(JobStatus.STARTED);
        jobRepository.create(runningJobToday);

        // act
        int todayJobs = jobRepository.countCompletedJobsToday();

        // assert
        Assertions.assertEquals(countCompletedJobsToday + 5, todayJobs);
    }

    @Test
    public void test4() {
        // arrange
        JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
        taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        taskRepository.setObjectMapper(createObjectMapper());

        JdbcJobRepository jobRepository = new JdbcJobRepository();
        jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
        jobRepository.setJobTaskRepository(taskRepository);

        int countCompletedJobsYesterday = jobRepository.countCompletedJobsYesterday();

        for (int i = 0; i < 5; i++) {
            SimpleJob completedJobYesterday = new SimpleJob();
            completedJobYesterday.setId(UUIDGenerator.generate() + "." + i);
            completedJobYesterday.setWorkflowId("demo:1234");
            completedJobYesterday.setCreateTime(Date.from(Instant.now().minus(2, DAYS)));
            completedJobYesterday.setStatus(JobStatus.COMPLETED);
            jobRepository.create(completedJobYesterday);
            completedJobYesterday.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
            jobRepository.merge(completedJobYesterday);
        }

        SimpleJob runningJobYesterday = new SimpleJob();
        runningJobYesterday.setId(UUIDGenerator.generate());
        runningJobYesterday.setWorkflowId("demo:1234");
        runningJobYesterday.setCreateTime(Date.from(Instant.now().minus(1, DAYS)));
        runningJobYesterday.setStatus(JobStatus.STARTED);
        jobRepository.create(runningJobYesterday);

        SimpleJob completedJobToday = new SimpleJob();
        completedJobToday.setId(UUIDGenerator.generate());
        completedJobToday.setWorkflowId("demo:1234");
        completedJobToday.setCreateTime(Date.from(Instant.now().minus(1, DAYS)));
        completedJobToday.setStatus(JobStatus.COMPLETED);
        jobRepository.create(completedJobToday);
        completedJobToday.setEndTime(new Date());
        jobRepository.merge(completedJobToday);

        // act
        int yesterdayJobs = jobRepository.countCompletedJobsYesterday();

        // assert
        Assertions.assertEquals(countCompletedJobsYesterday + 5, yesterdayJobs);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"));
        return objectMapper;
    }
}
