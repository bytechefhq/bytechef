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

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Executes an ffprobe command on a given input. 
 * 
 * @author Arik Cohen
 * @since May 23, 2017
 */
@Component("media/ffprobe")
class Ffprobe implements TaskHandler<Map<String,Object>> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper json = new ObjectMapper();
  
  @Override
  public Map<String,Object> handle(TaskExecution aTask) throws Exception {
    CommandLine cmd = new CommandLine ("ffprobe");
    cmd.addArgument("-v")
       .addArgument("quiet")
       .addArgument("-print_format")
       .addArgument("json")
       .addArgument("-show_error")
       .addArgument("-show_format")
       .addArgument("-show_streams")
       .addArgument(aTask.getRequiredString("input"));
    log.debug("{}",cmd);
    DefaultExecutor exec = new DefaultExecutor();
    File tempFile = File.createTempFile("log", null);
    try (PrintStream stream = new PrintStream(tempFile);) {
      exec.setStreamHandler(new PumpStreamHandler(stream));
      exec.execute(cmd);
      return parse(FileUtils.readFileToString(tempFile));
    }
    catch (ExecuteException e) {
      throw new ExecuteException(e.getMessage(),e.getExitValue(), new RuntimeException(FileUtils.readFileToString(tempFile)));
    }
    finally {
      FileUtils.deleteQuietly(tempFile);
    }
  }
  
  private Map<String,Object> parse (String aJson) throws Exception {
    Map<String,Object> result = json.readValue(aJson,Map.class);
    ArrayList<Object> video = new ArrayList<>();
    ArrayList<Object> audio = new ArrayList<>();
    result.put("video", video);
    result.put("audio", audio);
    List<Map<String,Object>> streams = (List<Map<String, Object>>) result.get("streams");
    for(int i=0; streams!=null&&i<streams.size(); i++) {
     Map<String, Object> stream = streams.get(i);
     if("video".equals(stream.get("codec_type"))) {
       video.add(stream);
     }
     else if("audio".equals(stream.get("codec_type"))) {
       audio.add(stream);
     }
    }
    return result;
  }

}