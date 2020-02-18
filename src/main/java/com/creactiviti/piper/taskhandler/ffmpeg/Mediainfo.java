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
package com.creactiviti.piper.taskhandler.ffmpeg;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * 
 * @author Arik Cohen
 * @since June 2, 2017
 */
@Component
public class Mediainfo implements TaskHandler<Map<String,Object>> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  
  @Override
  public Map<String,Object> handle (Task aTask) throws Exception {
    CommandLine cmd = new CommandLine ("mediainfo");
    cmd.addArgument(aTask.getRequiredString("input"));
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
  
  private Map<String,Object> parse (String aRaw) throws Exception {
    String[] lines = aRaw.split("\n");
    Map<String,Object> parsed = new HashMap<> ();
    String prefix = "";
    for(String line : lines) {
      String[] parts = line.split(":",2);
      if(parts.length == 1) {
        prefix = parts[0].trim().toLowerCase().replaceAll(" ", "_")+"_";
      }
      else {
        parsed.put(prefix+parts[0].trim().toLowerCase().replaceAll(" ", "_"), parts[1].trim());
      }
    }
    return parsed;
  }

}