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
package com.creactiviti.piper.web;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.creactiviti.piper.core.Coordinator;
import com.creactiviti.piper.core.Page;
import com.creactiviti.piper.core.annotations.ConditionalOnCoordinator;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobRepository;
import com.creactiviti.piper.core.job.JobSummary;

@RestController
@ConditionalOnCoordinator
public class JobsController {

  @Autowired private JobRepository jobRepository;
  @Autowired private Coordinator coordinator;
  
  @GetMapping(value="/jobs")
  public Page<JobSummary> list (@RequestParam(value="p",defaultValue="1") Integer aPageNumber) {
    return jobRepository.getPage(aPageNumber);
  }
  
  @PostMapping("/jobs")
  public Job create (@RequestBody Map<String, Object> aJobRequest) {
    return coordinator.create(aJobRequest);
  }
  
  @GetMapping(value="/jobs/{id}")
  public Job get (@PathVariable("id")String aJobId) {
    Job job = jobRepository.getById (aJobId);
    return job;
  }
  
  @GetMapping(value="/jobs/latest")
  public Job latest () {
    Optional<Job> job = jobRepository.getLatest();
    Assert.isTrue(job.isPresent(),"no jobs");
    return job.get();
  }
  
  @ExceptionHandler(IllegalArgumentException.class)
  public void handleIllegalArgumentException (HttpServletResponse aResponse) throws IOException {
    aResponse.sendError(HttpStatus.BAD_REQUEST.value());
  }
  
  @PutMapping(value="/jobs/{id}/restart")
  public Job restart (@PathVariable("id")String aJobId) {
    return coordinator.resume(aJobId);
  }
  
  @PutMapping(value="/jobs/{id}/stop")
  public Job step (@PathVariable("id")String aJobId) {
    return coordinator.stop(aJobId);
  }
    
}
