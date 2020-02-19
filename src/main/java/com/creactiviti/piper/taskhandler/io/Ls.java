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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("io/ls")
class Ls implements TaskHandler<List<Ls.FileInfo>> {

  @Override
  public List<Ls.FileInfo> handle (Task aTask) throws IOException {
    
    Path root = Paths.get(aTask.getRequiredString("path"));
    
    boolean recursive = aTask.getBoolean("recursive", false);
    
    return Files.walk(root)
                .filter(p->recursive || p.getParent().equals(root))
                .filter(Files::isRegularFile)
                .map(p->new FileInfo(root, p))
                .collect(Collectors.toList());
  }
  
  public static class FileInfo {
    
    private final Path path;
    private final Path root;
    
    public FileInfo(Path aRoot, Path aPath) {
      root = aRoot;
      path = aPath;
    }
    
    public String getName () {
      return path.getFileName().toString();
    }
    
    public String getRelativePath () {
      return root.relativize(path).toString();
    }
    
    public long getSize () throws IOException {
      return Files.size(path);
    }
    
  }

}
