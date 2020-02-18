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
import java.util.List;

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
 * a {@link TaskHandler} implementation which is used
 * for executing ffmpeg-based commands.
 * 
 * @author Arik Cohen
 * @since Jan 30, 2017
 */
@Component
public class Ffmpeg implements TaskHandler<Object> {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Object handle(Task aTask) throws Exception {
    List<String> options = aTask.getList("options", String.class);
    CommandLine cmd = new CommandLine ("ffmpeg");
    options.forEach(o->cmd.addArgument(o));
    log.debug("{}",cmd);
    DefaultExecutor exec = new DefaultExecutor();
    File tempFile = File.createTempFile("log", null);
    try (PrintStream stream = new PrintStream(tempFile);) {
      exec.setStreamHandler(new PumpStreamHandler(stream));
      int exitValue = exec.execute(cmd);
      return exitValue!=0?FileUtils.readFileToString(tempFile):cmd.toString();
    }
    catch (ExecuteException e) {
      throw new ExecuteException(e.getMessage(),e.getExitValue(), new RuntimeException(FileUtils.readFileToString(tempFile)));
    }
    finally {
      FileUtils.deleteQuietly(tempFile);
    }
  }

}
