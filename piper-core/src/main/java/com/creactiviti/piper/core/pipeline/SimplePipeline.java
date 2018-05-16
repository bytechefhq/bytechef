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
package com.creactiviti.piper.core.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.MapObject;
import com.creactiviti.piper.core.error.Error;
import com.creactiviti.piper.core.error.ErrorObject;
import com.creactiviti.piper.core.task.PipelineTask;

public class SimplePipeline extends MapObject implements Pipeline {

  public SimplePipeline(Map<String, Object> aSource) {
    super(aSource);
  }

  @Override
  public String getId() {
    return getString(DSL.ID);
  }

  @Override
  public String getLabel() {
    return getString(DSL.LABEL);
  }

  @Override
  public List<PipelineTask> getTasks() {
    return getList(DSL.TASKS, PipelineTask.class);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public List<Accessor> getInputs() {
    return getList(DSL.INPUTS,Accessor.class,Collections.EMPTY_LIST);
  }
  
  @Override
  public List<Accessor> getOutputs() {
    return getList(DSL.OUTPUTS,Accessor.class,Collections.EMPTY_LIST);
  }
  
  @Override
  public Error getError() {
    if(containsKey(DSL.ERROR)) {
      return new ErrorObject(getMap(DSL.ERROR));
    }
    return null;
  }
  
  public void setError (Error aError) {
    set(DSL.ERROR, aError);
  }
}
