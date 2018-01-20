
package com.creactiviti.piper.core.taskhandler.script;

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

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * a {@link TaskHandler} implementaion which lets one 
 * run arbitrary Bash scripts.
 * 
 * @author Arik Cohen
 * @since Apr 14, 2017
 */
@Component
public class Bash implements TaskHandler<String> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public String handle(Task aTask) throws Exception {
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
