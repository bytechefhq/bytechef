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
package com.creactiviti.piper.config;

public class CoordinatorSubscriptions {

  private int completions = 1;
  private int errors = 1;
  private int events = 1;
  private int jobs = 1;
  private int subflows = 1;

  public int getCompletions() {
    return completions;
  }
  
  public void setCompletions(int aCompletions) {
    completions = aCompletions;
  }

  public int getErrors() {
    return errors;
  }
  
  public void setErrors(int aErrors) {
    errors = aErrors;
  }
  
  public int getEvents() {
    return events;
  }
  
  public void setEvents(int aEvents) {
    events = aEvents;
  }
  
  public int getJobs() {
    return jobs;
  }
  
  public void setJobs(int aJobs) {
    jobs = aJobs;
  }

  public int getSubflows() {
    return subflows;
  }
  
  public void setSubflows(int aSubflows) {
    subflows = aSubflows;
  }
}
