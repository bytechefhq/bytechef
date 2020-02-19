package com.creactiviti.piper.taskhandler.io;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.google.common.io.Files;

public class RmTests {
  
  @Test
  public void test1 () throws IOException {
    Rm rm = new Rm();
    SimpleTaskExecution task = new SimpleTaskExecution();
    File tempDir = Files.createTempDir();
    Assertions.assertTrue(tempDir.exists());
    task.set("path", tempDir);
    rm.handle(task);
    Assertions.assertFalse(tempDir.exists());
  }
  
    
}
