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
package com.creactiviti.piper.core.metrics;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.annotations.ConditionalOnCoordinator;
import com.creactiviti.piper.core.job.JobRepository;


/**
 * a {@link PublicMetrics} implementaion which calculates 
 * the number of jobs currently running.
 * 
 * @author Arik Cohen
 * @since Apr 8, 2017
 */
@Component
@ConditionalOnCoordinator
public class RunningJobsMetrics implements PublicMetrics {

  @Autowired
  private JobRepository jobRepository;
  
  @Override
  public Collection<Metric<?>> metrics() {
    return Collections.singletonList(new Metric("jobs.running", jobRepository.countRunningJobs()));
  }

}
