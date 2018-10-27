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
 */
package com.creactiviti.piper.core.job;

import com.creactiviti.piper.config.MongoPersistenceConfiguration;
import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static com.creactiviti.piper.core.job.MongoJobRepository.DEFAULT_PAGE_SIZE;
import static com.creactiviti.piper.core.job.MongoJobRepositoryTests.MongoJobRepositoryTestsContextConfiguration;
import static com.creactiviti.piper.core.job.MongoJobRepositoryTests.MongodbInitailizer;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static de.flapdoodle.embed.process.runtime.Network.localhostIsIPv6;
import static java.text.MessageFormat.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    MongoPersistenceConfiguration.class,
    MongoJobRepositoryTestsContextConfiguration.class},
    properties = {"piper.persistence.provider=mongo"}
)
@ContextConfiguration(initializers = MongodbInitailizer.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class MongoJobRepositoryTests {
  @TestConfiguration
  static class MongoJobRepositoryTestsContextConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper()
          .configure(WRITE_DATES_AS_TIMESTAMPS, false)
          .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"));
    }
  }

  public static class MongodbInitailizer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      int randomPort = SocketUtils.findAvailableTcpPort();
      addInlinedPropertiesToEnvironment(applicationContext, format("spring.datasource.url=mongodb://{0}:{1,number,0}/piper", "localhost", randomPort));

      try {
        MongodExecutable mongodExe = starter
            .prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(randomPort, localhostIsIPv6()))
                .build());

        MongodProcess mongod = mongodExe.start();

        applicationContext.addApplicationListener(e -> {
          if (e instanceof ContextClosedEvent) {
            mongod.stop();
            mongodExe.stop();
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static final MongodStarter starter = MongodStarter.getDefaultInstance();
  private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

  @Autowired
  MongoJobRepository jobRepository;

  @Test
  public void test1() {
    // arrange
    Date now = new Date();

    SimpleJob job = new SimpleJob();
    job.setPipelineId("demo:1234");
    job.setId("1");
    job.setCreateTime(now);
    job.setStatus(JobStatus.STARTED);
    job.set(DSL.START_TIME, dateFormatter.format(now));

    // act
    jobRepository.create(job);
    Job one = jobRepository.findOne("1");

    // assert
    assertNotNull(one);
    assertEquals(now, one.getCreateTime());
    assertEquals(JobStatus.STARTED, one.getStatus());
    assertEquals(now, ((Map) one).get(DSL.START_TIME));
  }


  @Test
  public void test2() {
    // arrange
    Date now = new Date();

    int total = DEFAULT_PAGE_SIZE + 3;
    for (int i = 0; i < total; i++) {
      SimpleJob job = new SimpleJob();
      job.setPipelineId("demo:1234");
      job.setId("" + i);
      job.setCreateTime(now);
      job.setStatus(JobStatus.CREATED);
      jobRepository.create(job);
    }
    // act
    Page<Job> all = jobRepository.findAll(1);

    // assert
    assertEquals(DEFAULT_PAGE_SIZE, all.getSize());
    assertEquals(1, all.getNumber());
    assertEquals(20, all.getSize());
    assertEquals(total, all.getTotalItems());
  }

  @Test
  public void test3() {
    SimpleJob job = new SimpleJob();
    job.setId("2");
    job.setPipelineId("demo:1234");
    job.setCreateTime(new Date());
    job.setStatus(JobStatus.CREATED);
    jobRepository.create(job);

    Job one = jobRepository.findOne("2");

    SimpleJob mjob = new SimpleJob(one);
    mjob.setStatus(JobStatus.FAILED);

    // test immutability
    Assert.assertNotEquals(mjob.getStatus().toString(), one.getStatus().toString());

    jobRepository.merge(mjob);
    one = jobRepository.findOne("2");
    assertEquals("FAILED", one.getStatus().toString());
  }

  @Test
  public void test4() {
    // arrange
    SimpleJob completedJobYesterday = new SimpleJob();
    completedJobYesterday.setId("1");
    completedJobYesterday.setPipelineId("demo:1234");
    completedJobYesterday.setCreateTime(Date.from(Instant.now().minus(2, DAYS)));
    completedJobYesterday.setStatus(JobStatus.COMPLETED);
    jobRepository.create(completedJobYesterday);
    completedJobYesterday.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
    jobRepository.merge(completedJobYesterday);

    for (int i = 0; i < 5; i++) {
      SimpleJob completedJobToday = new SimpleJob();
      completedJobToday.setId("2." + i);
      completedJobToday.setPipelineId("demo:1234");
      completedJobToday.setCreateTime(Date.from(Instant.now().minus(1, DAYS)));
      completedJobToday.setStatus(JobStatus.COMPLETED);
      jobRepository.create(completedJobToday);
      completedJobToday.setEndTime(new Date());
      jobRepository.merge(completedJobToday);
    }

    SimpleJob runningJobToday = new SimpleJob();
    runningJobToday.setId("3");
    runningJobToday.setPipelineId("demo:1234");
    runningJobToday.setCreateTime(new Date());
    runningJobToday.setStatus(JobStatus.STARTED);
    jobRepository.create(runningJobToday);

    // act
    int todayJobs = jobRepository.countCompletedJobsToday();

    // assert
    Assert.assertEquals(5, todayJobs);
  }

  @Test
  public void test5() {
    // arrange
    for (int i = 0; i < 5; i++) {
      SimpleJob completedJobYesterday = new SimpleJob();
      completedJobYesterday.setId("1." + i);
      completedJobYesterday.setPipelineId("demo:1234");
      completedJobYesterday.setCreateTime(Date.from(Instant.now().minus(2, DAYS)));
      completedJobYesterday.setStatus(JobStatus.COMPLETED);
      jobRepository.create(completedJobYesterday);
      completedJobYesterday.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
      jobRepository.merge(completedJobYesterday);
    }

    SimpleJob runningJobYesterday = new SimpleJob();
    runningJobYesterday.setId("2");
    runningJobYesterday.setPipelineId("demo:1234");
    runningJobYesterday.setCreateTime(Date.from(Instant.now().minus(1, DAYS)));
    runningJobYesterday.setStatus(JobStatus.STARTED);
    jobRepository.create(runningJobYesterday);

    SimpleJob completedJobToday = new SimpleJob();
    completedJobToday.setId("3");
    completedJobToday.setPipelineId("demo:1234");
    completedJobToday.setCreateTime(Date.from(Instant.now().minus(1, DAYS)));
    completedJobToday.setStatus(JobStatus.COMPLETED);
    jobRepository.create(completedJobToday);
    completedJobToday.setEndTime(new Date());
    jobRepository.merge(completedJobToday);

    // act
    int yesterdayJobs = jobRepository.countCompletedJobsYesterday();

    // assert
    Assert.assertEquals(5, yesterdayJobs);
  }

}
