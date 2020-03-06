
package com.creactiviti.piper.core;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.creactiviti.piper.core.context.JdbcContextRepository;
import com.creactiviti.piper.core.job.JdbcJobRepository;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobStatus;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.messagebroker.SyncMessageBroker;
import com.creactiviti.piper.core.pipeline.ResourceBasedPipelineRepository;
import com.creactiviti.piper.core.task.DefaultTaskHandlerResolver;
import com.creactiviti.piper.core.task.JdbcTaskExecutionRepository;
import com.creactiviti.piper.core.task.SpelTaskEvaluator;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.WorkTaskDispatcher;
import com.creactiviti.piper.taskhandler.io.Print;
import com.creactiviti.piper.taskhandler.random.RandomInt;
import com.creactiviti.piper.taskhandler.time.Sleep;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;


@SpringBootTest
public class CoordinatorTests {

  @Autowired
  private DataSource dataSource;
  
  @Test
  public void testStartJob () throws SQLException {
    Coordinator coordinator = new Coordinator ();
   
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (o)->coordinator.complete((TaskExecution)o));
    messageBroker.receive(Queues.JOBS, (o)->coordinator.start((Job)o));
    
    Map<String,TaskHandler<?>> handlers = new HashMap<>();
    handlers.put("io/print", new Print());
    handlers.put("random/int", new RandomInt());
    handlers.put("time/sleep", new Sleep());
    
    DefaultTaskHandlerResolver taskHandlerResolver = new DefaultTaskHandlerResolver(handlers);
    
    Worker worker = Worker.builder()
                          .withTaskHandlerResolver(taskHandlerResolver)
                          .withMessageBroker(messageBroker)
                          .withEventPublisher((e)->{})
                          .withTaskEvaluator(SpelTaskEvaluator.create())
                          .build();
    
    ObjectMapper objectMapper = createObjectMapper();
    
    JdbcContextRepository contextRepository = new JdbcContextRepository();
    contextRepository.setJdbcTemplate(new JdbcTemplate(dataSource));
    contextRepository.setObjectMapper(objectMapper);
    
    coordinator.setContextRepository(contextRepository);
        
    JdbcTaskExecutionRepository taskRepository = new JdbcTaskExecutionRepository();
    taskRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    taskRepository.setObjectMapper(createObjectMapper());
    
    JdbcJobRepository jobRepository = new JdbcJobRepository();
    jobRepository.setJdbcOperations(new NamedParameterJdbcTemplate(dataSource));
    jobRepository.setJobTaskRepository(taskRepository);
    
    coordinator.setJobRepository(jobRepository);
    coordinator.setPipelineRepository(new ResourceBasedPipelineRepository());
    coordinator.setJobTaskRepository(taskRepository);
    
    SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();
    coordinatorMessageBroker.receive(Queues.TASKS, (o)->worker.handle((TaskExecution)o));
    WorkTaskDispatcher taskDispatcher = new WorkTaskDispatcher(coordinatorMessageBroker);
    coordinator.setTaskDispatcher(taskDispatcher);
    coordinator.setEventPublisher((e)->{});
    
    DefaultJobExecutor jobExecutor = new DefaultJobExecutor ();
    jobExecutor.setContextRepository(contextRepository);
    jobExecutor.setJobTaskRepository(taskRepository);
    jobExecutor.setPipelineRepository(new ResourceBasedPipelineRepository());
    jobExecutor.setTaskDispatcher(taskDispatcher);
    jobExecutor.setTaskEvaluator(SpelTaskEvaluator.create());
    coordinator.setJobExecutor(jobExecutor);
    
    DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler();
    taskCompletionHandler.setContextRepository(contextRepository);
    taskCompletionHandler.setJobExecutor(jobExecutor);
    taskCompletionHandler.setJobRepository(jobRepository);
    taskCompletionHandler.setJobTaskRepository(taskRepository);
    taskCompletionHandler.setPipelineRepository(new ResourceBasedPipelineRepository());
    taskCompletionHandler.setEventPublisher((e)->{});
    taskCompletionHandler.setTaskEvaluator(SpelTaskEvaluator.create());
    coordinator.setTaskCompletionHandler(taskCompletionHandler);
    coordinator.setMessageBroker(messageBroker);
        
    Job job = coordinator.create(MapObject.of(ImmutableMap.of("pipelineId","demo/hello","inputs",Collections.singletonMap("yourName","me"))));
    
    Job completedJob = jobRepository.getById(job.getId());
    
    Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
    return objectMapper;
  }
  
  @Test
  public void testRequiredParams () {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Coordinator coordinator = new Coordinator ();
      coordinator.setPipelineRepository(new ResourceBasedPipelineRepository());
      coordinator.create(MapObject.of(Collections.singletonMap("pipelineId","demo/hello")));
    });
  }
  
}