package com.creactiviti.piper.core.job;

import com.creactiviti.piper.core.Page;
import com.creactiviti.piper.core.task.JdbcTaskExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;


@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class JdbcJobRepositoryTests {

  @Autowired
  private DataSource dataSource; 
  
  @Test
  public void test1 () {
    JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
    taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    taskRepository.setObjectMapper(createObjectMapper());
    
    JdbcJobRepository jobRepository = new JdbcJobRepository();
    jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    jobRepository.setJobTaskRepository(taskRepository);
    
    int pageTotal = jobRepository.findAll(1).getNumber();
    
    SimpleJob job = new SimpleJob();
    job.setPipelineId("demo:1234");
    job.setId("1");
    job.setCreateTime(new Date());
    job.setStatus(JobStatus.CREATED);
    jobRepository.create(job);
    
    Page<Job> all = jobRepository.findAll(1);
    Assert.assertEquals(pageTotal+1,all.getSize());
    
    Job one = jobRepository.findOne("1");
    Assert.assertNotNull(one);
  }
  
  @Test
  public void test2 () {
    JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
    taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    taskRepository.setObjectMapper(createObjectMapper());
    
    JdbcJobRepository jobRepository = new JdbcJobRepository();
    jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    jobRepository.setJobTaskRepository(taskRepository);
    
    SimpleJob job = new SimpleJob();
    job.setId("1");
    job.setPipelineId("demo:1234");
    job.setCreateTime(new Date());
    job.setStatus(JobStatus.CREATED);
    jobRepository.create(job);
    
    Job one = jobRepository.findOne("1");
    
    SimpleJob mjob = new SimpleJob(one);
    mjob.setStatus(JobStatus.FAILED);
    
    // test immutability
    Assert.assertNotEquals(mjob.getStatus().toString(),one.getStatus().toString());  
    
    jobRepository.merge(mjob);
    one = jobRepository.findOne("1");
    Assert.assertEquals("FAILED",one.getStatus().toString());  
  }

  @Test
  public void test3 () {
    // arrange
    JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
    taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    taskRepository.setObjectMapper(createObjectMapper());

    JdbcJobRepository jobRepository = new JdbcJobRepository();
    jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    jobRepository.setJobTaskRepository(taskRepository);

    SimpleJob completedJobYesterday = new SimpleJob();
    completedJobYesterday.setId("1");
    completedJobYesterday.setPipelineId("demo:1234");
    completedJobYesterday.setCreateTime(Date.from(Instant.now().minus(2, DAYS)));
    completedJobYesterday.setStatus(JobStatus.COMPLETED);
    jobRepository.create(completedJobYesterday);
    completedJobYesterday.setEndTime(Date.from(Instant.now().minus(1, DAYS)));
    jobRepository.merge(completedJobYesterday);

    for(int i = 0; i < 5; i++) {
        SimpleJob completedJobToday = new SimpleJob();
        completedJobToday.setId("2."+i);
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
  public void test4 () {
    // arrange
    JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
    taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    taskRepository.setObjectMapper(createObjectMapper());

    JdbcJobRepository jobRepository = new JdbcJobRepository();
    jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    jobRepository.setJobTaskRepository(taskRepository);

    for(int i = 0; i < 5; i++) {
        SimpleJob completedJobYesterday = new SimpleJob();
        completedJobYesterday.setId("1."+i);
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

  private ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"));
    return objectMapper;
  }
  
}
