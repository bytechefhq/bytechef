package com.creactiviti.piper.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskExecutionRepository;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
class InMemoryTaskExecutionRepository implements TaskExecutionRepository {
  
  private final Map<String, TaskExecution> executions = new HashMap<> ();

  @Override
  public TaskExecution findOne(String aId) {
    TaskExecution taskExecution = executions.get(aId);
    Assert.notNull(taskExecution,"unknown task execution: " + aId);
    return taskExecution;
  }

  @Override
  public List<TaskExecution> findByParentId(String aParentId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void create(TaskExecution aTaskExecution) { 
    Assert.isTrue(executions.get(aTaskExecution.getId())==null,"task execution " + aTaskExecution.getId() + " already exists");
    executions.put(aTaskExecution.getId(), aTaskExecution);
  }

  @Override
  public TaskExecution merge (TaskExecution aTaskExecution) {
    executions.put(aTaskExecution.getId(), aTaskExecution);
    return aTaskExecution;
  }

  @Override
  public List<TaskExecution> getExecution(String aJobId) {
    return Collections.unmodifiableList(new ArrayList<>(executions.values()));
  }

}
