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
 *//* 
 * Copyright (C) Creactiviti LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Arik Cohen <arik@creactiviti.com>, June 2017
 */
package com.creactiviti.piper.taskhandler.media;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.arakelian.jq.ImmutableJqLibrary;
import com.arakelian.jq.ImmutableJqRequest;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("media/vduration")
class Vduration implements TaskHandler<Double> {

  private final Ffprobe ffprobe = new Ffprobe();
  private final ObjectMapper jsonMapper = new ObjectMapper ();
  
  @Override
  public Double handle (TaskExecution aTask) throws Exception {
    Map<?,?> ffprobeResult = ffprobe.handle(aTask);
    
    // attempt to get the duration from the codec_type = video stream
    String output = ImmutableJqRequest.builder()
        .lib(ImmutableJqLibrary.of())
        .input(jsonMapper.writeValueAsString(ffprobeResult))
        .filter(".streams[] | select (.codec_type==\"video\") | .duration")
        .build()
        .execute()
        .getOutput();
    
    // fallback to the container's format.duration
    if(StringUtils.isBlank(output) || output.equals("null")) {
      output = ImmutableJqRequest.builder() 
          .lib(ImmutableJqLibrary.of())
          .input(jsonMapper.writeValueAsString(ffprobeResult))
          .filter(".format.duration")
          .build()
          .execute()
          .getOutput();
    }
    
    return Double.valueOf(output.replaceAll("[^0-9\\.]", ""));
  }
  
}

