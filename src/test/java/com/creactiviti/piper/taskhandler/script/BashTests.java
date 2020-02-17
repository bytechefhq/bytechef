package com.creactiviti.piper.taskhandler.script;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.taskhandler.script.Bash;

public class BashTests {

  @Test
  public void test1 () throws Exception {
    Bash bash = new Bash();
    ClassPathResource cpr = new ClassPathResource("schema.sql");
    String output = bash.handle(SimpleTaskExecution.createFrom ("script", "ls -l " + cpr.getFile().getAbsolutePath()));
    Assertions.assertTrue(output.contains("target/classes/schema.sql"));
  }
  
}
