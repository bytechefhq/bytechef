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
package com.creactiviti.piper.taskhandler.media;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * 
 * @author Arik Cohen
 * @since June 2, 2017
 */
@Component("media/mediainfo")
class Mediainfo implements TaskHandler<Map<?,?>> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public Map<?,?> handle (TaskExecution aTask) throws Exception {

    String output = new ProcessExecutor()
        .command(List.of("mediainfo","--Output=JSON",aTask.getRequiredString("input")," | ", "xq"))
        .readOutput(true)
        .execute()
        .outputUTF8();

    log.debug("{}",output);
    
    return mapper.readValue(output, Map.class);
  }


}