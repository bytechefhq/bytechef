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

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;

@Component("media/vduration")
class Vduration implements TaskHandler<Double> {

  private Ffprobe ffprobe = new Ffprobe();
  
  @Override
  public Double handle (TaskExecution aTask) throws Exception {
    Map<String, Object> ffprobeResult = ffprobe.handle(aTask);
    List<Map<String,Object>> videos = (List<Map<String, Object>>) ffprobeResult.get("video");
    if(videos!=null && videos.size() > 0) {
      Map<String, Object> video = videos.get(0);
      Object duration = video.get("duration");
      if(duration instanceof String) {
        return Double.valueOf((String)duration);
      }
      return (Double) duration;
    }
    return null;
  }

}

