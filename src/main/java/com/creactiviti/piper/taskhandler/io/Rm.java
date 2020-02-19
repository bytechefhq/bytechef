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
package com.creactiviti.piper.taskhandler.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
 * 
 * <p>A directory to be deleted does not have to be empty.
 * 
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("io/rm")
class Rm implements TaskHandler<Object> {

  @Override
  public Object handle (Task aTask) throws IOException {
    File file = new File (aTask.getRequiredString("path"));
    if(!file.exists()) {
      return null;
    }
    boolean result = FileUtils.deleteQuietly(file);
    Assert.isTrue(result, "Failed to delete: " + aTask.getString("path"));
    return null;
  }
  
}
