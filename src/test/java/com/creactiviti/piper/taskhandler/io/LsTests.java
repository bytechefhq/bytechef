package com.creactiviti.piper.taskhandler.io;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.taskhandler.io.Ls.FileInfo;

public class LsTests {
  
  @Test
  public void test1 () throws IOException {
    Ls ls = new Ls();
    ClassPathResource cpr = new ClassPathResource("ls");
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.set("path", cpr.getFile().getAbsolutePath());
    task.set("recursive", true);
    List<FileInfo> files = ls.handle(task);
    Assertions.assertEquals(Set.of("C.txt","B.txt","A.txt"), files.stream()
                                                                  .map(FileInfo::getName)
                                                                  .collect(Collectors.toSet()));
  }
  
  @Test
  public void test2 () throws IOException {
    Ls ls = new Ls();
    ClassPathResource cpr = new ClassPathResource("ls");
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.set("path", cpr.getFile().getAbsolutePath());
    task.set("recursive", true);
    List<FileInfo> files = ls.handle(task);
    Assertions.assertEquals(Set.of("sub1/C.txt","B.txt","A.txt"), files.stream()
                                                                  .map(FileInfo::getRelativePath)
                                                                  .collect(Collectors.toSet()));
  }
  
  @Test
  public void test3 () throws IOException {
    Ls ls = new Ls();
    ClassPathResource cpr = new ClassPathResource("ls");
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.set("path", cpr.getFile().getAbsolutePath());
    List<FileInfo> files = ls.handle(task);
    Assertions.assertEquals(Set.of("B.txt","A.txt"), files.stream()
                                                                  .map(FileInfo::getName)
                                                                  .collect(Collectors.toSet()));
  }
  
}
