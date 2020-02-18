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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.Page;
import com.creactiviti.piper.core.ResultPage;
import com.creactiviti.piper.core.json.JsonHelper;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JdbcJobRepository implements JobRepository {

  private NamedParameterJdbcOperations jdbc;
  private TaskExecutionRepository jobTaskRepository;
  
  private final ObjectMapper json = new ObjectMapper();
  
  public static final int DEFAULT_PAGE_SIZE = 20;
  
  @Override
  public Job getById(String aId) {
    List<Job> query = jdbc.query("select * from job where id = :id", Collections.singletonMap("id", aId),this::jobRowMappper);
    Assert.isTrue(query.size() == 1, "expected 1 result. got " + query.size());
    return query.get(0);
  }
  
  @Override
  public Optional<Job> getLatest() {
    List<Job> query = jdbc.query("select * from job order by create_time desc limit 1", this::jobRowMappper);
    if(query.size() == 0) {
      return Optional.empty();
    }
    return Optional.of(query.get(0));
  }
  
  @Override
  public Job getByTaskId(String aTaskId) {
    Map<String, String> params = Collections.singletonMap("id", aTaskId);
    List<Job> list = jdbc.query("select * from job j where j.id = (select job_id from task_execution jt where jt.id=:id)", params, this::jobRowMappper);
    Assert.isTrue(list.size() < 2, "expecting 1 result, got: " + list.size());
    return list.size() == 1 ? list.get(0) : null;
  }

  @Override
  public Page<Job> getPage(int aPageNumber) {
    Integer totalItems = jdbc.getJdbcOperations().queryForObject("select count(*) from job",Integer.class);
    int offset = (aPageNumber-1) * DEFAULT_PAGE_SIZE;
    int limit = DEFAULT_PAGE_SIZE;
    List<Job> items = jdbc.query(String.format("select * from job order by create_time desc offset %s limit %s",offset,limit),this::jobRowMappper);
    ResultPage<Job> resultPage = new ResultPage<>(Job.class);
    resultPage.setItems(items);
    resultPage.setNumber(items.size()>0?aPageNumber:0);
    resultPage.setTotalItems(totalItems);
    resultPage.setTotalPages(items.size()>0?totalItems/DEFAULT_PAGE_SIZE+1:0);
    return resultPage;
  }
      
  @Override
  public Job merge (Job aJob) {
    MapSqlParameterSource sqlParameterSource = createSqlParameterSource(aJob);
    jdbc.update("update job set status=:status,start_time=:startTime,end_time=:endTime,current_task=:currentTask,pipeline_id=:pipelineId,label=:label,tags=:tags,outputs=:outputs where id = :id ", sqlParameterSource);
    return aJob;
  }

  @Override
  public void create (Job aJob) {
    MapSqlParameterSource sqlParameterSource = createSqlParameterSource(aJob);
    jdbc.update("insert into job (id,create_time,start_time,status,current_task,pipeline_id,label,tags,priority,inputs,webhooks,outputs,parent_task_execution_id) values (:id,:createTime,:startTime,:status,:currentTask,:pipelineId,:label,:tags,:priority,:inputs,:webhooks,:outputs,:parentTaskExecutionId)", sqlParameterSource);
  }

  private MapSqlParameterSource createSqlParameterSource(Job aJob) {
    SimpleJob job = new SimpleJob(aJob);
    Assert.notNull(aJob, "job must not be null");
    Assert.notNull(aJob.getId(), "job status must not be null");
    Assert.notNull(aJob.getCreateTime(), "job createTime must not be null");
    Assert.notNull(aJob.getStatus(), "job status must not be null");
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("id", job.getId());
    sqlParameterSource.addValue("status", job.getStatus().toString());
    sqlParameterSource.addValue("currentTask", job.getCurrentTask());
    sqlParameterSource.addValue("pipelineId", job.getPipelineId());
    sqlParameterSource.addValue("label", job.getLabel());
    sqlParameterSource.addValue("createTime", job.getCreateTime());
    sqlParameterSource.addValue("startTime", job.getStartTime());
    sqlParameterSource.addValue("endTime", job.getEndTime());
    sqlParameterSource.addValue("tags", String.join(",",job.getTags()));
    sqlParameterSource.addValue("priority", job.getPriority());
    sqlParameterSource.addValue("inputs", JsonHelper.writeValueAsString(json,job.getInputs()));
    sqlParameterSource.addValue("outputs", JsonHelper.writeValueAsString(json,job.getOutputs()));
    sqlParameterSource.addValue("webhooks", JsonHelper.writeValueAsString(json,job.getWebhooks()));
    sqlParameterSource.addValue("parentTaskExecutionId", job.getParentTaskExecutionId());
    return sqlParameterSource;
  }
  
  public void setJobTaskRepository(TaskExecutionRepository aJobTaskRepository) {
    jobTaskRepository = aJobTaskRepository;
  }
    
  public void setJdbcOperations (NamedParameterJdbcOperations aJdbcOperations) {
    jdbc = aJdbcOperations;
  }

  private Job jobRowMappper (ResultSet aRs, int aIndex) throws SQLException {
    Map<String, Object> map = new HashMap<>();
    map.put("id", aRs.getString("id"));
    map.put("status", aRs.getString("status"));
    map.put("currentTask", aRs.getInt("current_task"));
    map.put("pipelineId", aRs.getString("pipeline_id"));
    map.put("label", aRs.getString("label"));
    map.put("createTime", aRs.getTimestamp("create_time"));
    map.put("startTime", aRs.getTimestamp("start_time"));
    map.put("endTime", aRs.getTimestamp("end_time"));
    map.put("execution", getExecution(aRs.getString("id")));
    map.put("tags", aRs.getString("tags").length()>0?aRs.getString("tags").split(","):new String[0]);
    map.put("priority", aRs.getInt("priority"));
    map.put("inputs", JsonHelper.readValue(json,aRs.getString("inputs"),Map.class));
    map.put("outputs", JsonHelper.readValue(json,aRs.getString("outputs"),Map.class));
    map.put("webhooks", JsonHelper.readValue(json,aRs.getString("webhooks"),List.class));
    map.put(DSL.PARENT_TASK_EXECUTION_ID, aRs.getString("parent_task_execution_id"));
    return new SimpleJob(map);
  }
  
  private List<TaskExecution> getExecution(String aJobId) {
    return jobTaskRepository.getExecution(aJobId);
  }

  @Override
  public int countRunningJobs() {
    return (int) jdbc.queryForObject("select count(*) from job where status='STARTED'", Collections.EMPTY_MAP, Integer.class);
  }

  @Override
  public int countCompletedJobsToday() {
    return (int)jdbc.queryForObject("select count(*) from job where status='COMPLETED' and end_time >= current_date", Collections.EMPTY_MAP, Integer.class);
  }

  @Override
  public int countCompletedJobsYesterday() {
    return (int)jdbc.queryForObject("select count(*) from job where status='COMPLETED' and end_time >= current_date-1 and end_time < current_date", Collections.EMPTY_MAP, Integer.class);
  }



}
