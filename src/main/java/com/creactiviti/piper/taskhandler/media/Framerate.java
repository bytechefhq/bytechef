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

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.arakelian.jq.ImmutableJqLibrary;
import com.arakelian.jq.ImmutableJqRequest;
import com.arakelian.jq.JqResponse;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author Arik Cohen
 * @since Jun 2, 2017
 */
@Component("media/framerate")
class Framerate implements TaskHandler<Double> {

  private final Ffprobe ffprobe = new Ffprobe();
  private final ObjectMapper jsonMapper = new ObjectMapper ();
  
  @Override
  public Double handle (TaskExecution aTask) throws Exception {
    Map<?,?> ffprobeResult = ffprobe.handle(aTask);
    
    JqResponse response = ImmutableJqRequest.builder() //
        .lib(ImmutableJqLibrary.of())
        .input(jsonMapper.writeValueAsString(ffprobeResult))
        .filter(".streams[] | select (.codec_type==\"video\") | .r_frame_rate") 
        .build()
        .execute();
    
    String frameRateStr = response.getOutput();
    Assert.notNull(frameRateStr, "can not determine framerate");
    String[] frate = frameRateStr.replaceAll("[^0-9/\\.]", "").split("/");
    return Double.valueOf(frate[0])/Double.valueOf(frate[1]);
  }
  
}

