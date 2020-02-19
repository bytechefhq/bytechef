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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * Creates a directory by creating all nonexistent parent directories first. 
 * 
 * <p>An exception is not thrown if the directory could not be created because it already exists.
 * 
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("io/mkdir")
class Mkdir implements TaskHandler<Object> {

  @Override
  public Object handle (Task aTask) throws IOException {
    Files.createDirectories(Paths.get(aTask.getRequiredString("path")));
    return null;
  }
  
}
