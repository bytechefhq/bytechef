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

import static org.apache.commons.io.IOUtils.copy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.event.EventPublisher;
import com.creactiviti.piper.core.event.Events;
import com.creactiviti.piper.core.event.PiperEvent;
import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;

/**
 * Simple task handler which performs the download of a file (given its URL).
 *
 * @since Sep 06, 2018
 */
@Component
class Download implements TaskHandler<Object> {

  private final EventPublisher eventPublisher;

  public Download (EventPublisher aEventPublisher) {
    eventPublisher = Objects.requireNonNull(aEventPublisher);
  }

  @Override
  public Object handle(Task aTask) {
    try {
      URL url = new URL(aTask.getRequiredString("url"));
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.connect();

      if (connection.getResponseCode() / 100 == 2) {
        File downloadedFile = File.createTempFile("download-", "", null);
        int contentLength = connection.getContentLength();
        Consumer<Integer> progressConsumer =
            (p) -> eventPublisher.publishEvent(PiperEvent.of(Events.TASK_PROGRESSED,"taskId", ((TaskExecution)aTask).getId(), "progress", p));

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                OutputStream os = new ProgressingOutputStream(new FileOutputStream(downloadedFile), contentLength, progressConsumer)) {

              copy(in, os);
            }
            return downloadedFile.toString();
      }

      throw new IllegalStateException("Server returned: " + connection.getResponseCode());

    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class ProgressingOutputStream extends FilterOutputStream {
    private final int totalSize;
    private final OutputStream out;
    private final Consumer<Integer> progressConsumer;
    private long count;
    private long lastTime;
    private long delta = 500;

    ProgressingOutputStream(OutputStream out, int totalSize, Consumer<Integer> progressConsumer) {
      super(out);

      this.totalSize = totalSize;
      this.out = out;
      this.progressConsumer = progressConsumer;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      this.out.write(b, off, len);
      this.count += len;

      report();
    }

    @Override
    public void write(int b) throws IOException {
      this.out.write(b);
      ++this.count;

      report();
    }

    void report() {
      if(totalSize != -1 && totalSize != -0) {
        long time = System.currentTimeMillis();

        if( count == totalSize ) {
          progressConsumer.accept(100);
        }

        if( time > lastTime + delta ) {
          int p = (int)(count * 100 / totalSize);
          progressConsumer.accept(p);
          lastTime = time;
        }
      }
    }

  }

}
