package com.creactiviti.piper.taskhandler.bento4;

import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("bento4/mp4hls")
class Mp4hls implements TaskHandler<Object> {
  
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public Object handle (Task aTask) throws Exception {
    List<String> options = aTask.getList("options", String.class);
    CommandLine cmd = new CommandLine ("mp4hls");
    options.forEach(o->cmd.addArgument(o));
    logger.debug("{}",cmd);
    DefaultExecutor exec = new DefaultExecutor();
    int exitValue = exec.execute(cmd);
    Assert.isTrue(exitValue == 0, "exit value: " + exitValue);
    return null;
  }

}
