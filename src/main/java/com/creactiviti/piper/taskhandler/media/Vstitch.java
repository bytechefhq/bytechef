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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * a {@link TaskHandler} implementation which "stitches" together a 
 * collection of <code>chunks</code> into a single file specified by 
 * the <code>output</code> property.
 * 
 * @author Arik Cohen
 * @since Jun 4, 2017
 */
@Component("media/vstitch")
class Vstitch implements TaskHandler<Object> {

  private final Ffmpeg ffmpeg = new Ffmpeg();
  
  @Override
  public Object handle (Task aTask) throws Exception {
    List<String> chunks = aTask.getList("chunks",String.class);
    File tempFile = File.createTempFile("_chunks", ".txt");
    try {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile, true), "UTF-8"))) {
        for(String chunk : chunks) {
          writer.append(String.format("file '%s'", chunk))
                .append("\n");
        }
      }
      SimpleTaskExecution ffmpegTask = SimpleTaskExecution.create();
      List<String> options = Arrays.asList(
        "-y",
        "-f","concat",
        "-safe","0",
        "-i",tempFile.getAbsolutePath(),
        "-c","copy",
        aTask.getRequiredString("output")
      );
      ffmpegTask.set("options", options);
      ffmpeg.handle(ffmpegTask);
    }
    finally {
      FileUtils.deleteQuietly(tempFile);
    }
    return null;
  }

}

