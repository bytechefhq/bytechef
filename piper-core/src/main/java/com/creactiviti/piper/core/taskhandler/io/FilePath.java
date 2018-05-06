
package com.creactiviti.piper.core.taskhandler.io;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * Gets the path from a full filename, which excludes the prefix, and also excluding the final directory separator. 
 * 
 * This method will handle a file in either Unix or Windows format. The method is entirely text based, and 
 * returns the text before the last forward or backslash. 
 * 
 * @author Arik Cohen
 * @since May 6, 2018
 */
@Component
public class FilePath implements TaskHandler<Object> {

  @Override
  public Object handle (Task aTask) {
    return FilenameUtils.getPathNoEndSeparator(aTask.getRequiredString("filename"));
  }

}
