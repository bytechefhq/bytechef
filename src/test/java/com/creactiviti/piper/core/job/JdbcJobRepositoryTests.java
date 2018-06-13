package com.creactiviti.piper.core.job;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.creactiviti.piper.core.Page;
import com.creactiviti.piper.core.task.JdbcTaskExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
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
    job.setId("2");
    job.setPipelineId("demo:1234");
    job.setCreateTime(new Date());
    job.setStatus(JobStatus.CREATED);
    jobRepository.create(job);
    
    Job one = jobRepository.findOne("2");
    
    SimpleJob mjob = new SimpleJob(one);
    mjob.setStatus(JobStatus.FAILED);
    
    // test immutability
    Assert.assertNotEquals(mjob.getStatus().toString(),one.getStatus().toString());  
    
    jobRepository.merge(mjob);
    one = jobRepository.findOne("2");
    Assert.assertEquals("FAILED",one.getStatus().toString());  
  }
  
  private ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"));
    return objectMapper;
  }
  
}
