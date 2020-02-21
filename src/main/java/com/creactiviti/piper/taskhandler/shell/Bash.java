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
package com.creactiviti.piper.taskhandler.shell;

import java.io.File;
import java.io.PrintStream;

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

/**
 * a {@link TaskHandler} implementaion which lets one 
 * run arbitrary Bash scripts.
 * 
 * @author Arik Cohen
 * @since Apr 14, 2017
 */
@Component("shell/bash")
class Bash implements TaskHandler<String> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public String handle(TaskExecution aTask) throws Exception {
    File scriptFile = File.createTempFile("_script", ".sh");
    File logFile = File.createTempFile("log", null);
    FileUtils.writeStringToFile(scriptFile, aTask.getRequiredString("script"));
    try (PrintStream stream = new PrintStream(logFile);) {
      Process chmod = Runtime.getRuntime().exec(String.format("chmod u+x %s",scriptFile.getAbsolutePath()));
      int chmodRetCode = chmod.waitFor();
      if(chmodRetCode != 0) {
        throw new ExecuteException("Failed to chmod", chmodRetCode);
      }
      CommandLine cmd = new CommandLine (scriptFile.getAbsolutePath());
      logger.debug("{}",cmd);
      DefaultExecutor exec = new DefaultExecutor();
      exec.setStreamHandler(new PumpStreamHandler(stream));
      exec.execute(cmd);
      return FileUtils.readFileToString(logFile);
    }
    catch (ExecuteException e) {
      throw new ExecuteException(e.getMessage(),e.getExitValue(), new RuntimeException(FileUtils.readFileToString(logFile)));
    }
    finally {
      FileUtils.deleteQuietly(logFile);
      FileUtils.deleteQuietly(scriptFile);
    }
  }
  
}
