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
package com.creactiviti.piper.core.task;

import java.util.List;
import java.util.Map;

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.MapObject;


public class SimplePipelineTask extends MapObject implements PipelineTask {

  public SimplePipelineTask (Task aSource) {
    super(aSource.asMap());
  }
  
  public SimplePipelineTask (Map<String, Object> aSource) {
    super(aSource);
  }
  
  @Override
  public String getType() {
    return getString(DSL.TYPE);
  }
  
  @Override
  public String getName() {
    return getString(DSL.NAME);
  }
  
  @Override
  public String getLabel() {
    return getString(DSL.LABEL);
  }

  @Override
  public String getNode() {
    return getString(DSL.NODE);
  }
  
  public void setNode (String aNode) {
    set(DSL.NODE, aNode);
  }
  
  @Override
  public int getTaskNumber() {
    return getInteger(DSL.TASK_NUMBER,-1);
  }
  
  public void setTaskNumber (int aTaskNumber) {
    set(DSL.TASK_NUMBER, aTaskNumber);
  }
  
  @Override
  public String getTimeout() {
    return getString(DSL.TIMEOUT);
  }
  
  @Override
  public List<PipelineTask> getPre() {
    return getList(DSL.PRE, PipelineTask.class, List.of());
  }
  
  @Override
  public List<PipelineTask> getPost() {
    return getList(DSL.POST, PipelineTask.class, List.of());
  }
  
  @Override
  public List<PipelineTask> getFinalize() {
    return getList(DSL.FINALIZE, PipelineTask.class, List.of());
  }
  
}