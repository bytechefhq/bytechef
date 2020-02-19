package com.creactiviti.piper.taskhandler.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.task.SimpleTaskExecution;

public class MkdirTests {
  
  @Test
  public void test1 () throws IOException {
    Mkdir mkdir = new Mkdir();
    SimpleTaskExecution task = SimpleTaskExecution.create();
    String tempDir = System.getProperty("java.io.tmpdir") + "/"  + RandomStringUtils.randomAlphabetic(10);
    task.set("path", tempDir);
    mkdir.handle(task);
    Assertions.assertTrue(new File(tempDir).exists());
  }
  
  @Test
  public void test2 () throws IOException {
    Assertions.assertThrows(FileSystemException.class,() -> {
      Mkdir mkdir = new Mkdir();
      SimpleTaskExecution task = SimpleTaskExecution.create();
      task.set("path", "/no/such/thing");
      mkdir.handle(task);
    });
  }
    
}
