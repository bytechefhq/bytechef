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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;


/**
 * 
 * @author Arik Cohen
 * @since June 2, 2017
 */
@Component("media/mediainfo")
class Mediainfo implements TaskHandler<Mediainfo.Output> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  
  private static final XmlMapper xmlMapper = new XmlMapper();
  
  @Override
  public Mediainfo.Output handle (TaskExecution aTask) throws Exception {
    
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    String output = new ProcessExecutor()
                    .command(List.of("mediainfo","--Output=XML",aTask.getRequiredString("input")))
                    .readOutput(true)
                    .execute()
                    .outputUTF8();
    
    log.debug("{}",output);
    
    return xmlMapper.readValue(output,Output.class);
  }
  
  static class Output {
    
    private List<Map<?,?>> media;
    
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    
    public List<Map<?, ?>> getMedia() {
      return media;
    }
    
    public void setMedia(List<Map<?, ?>> aMedia) {
      media = aMedia;
    }
    
    public String toJson () throws JsonProcessingException {
      return jsonMapper.writeValueAsString(this);
    }
    
  }

}