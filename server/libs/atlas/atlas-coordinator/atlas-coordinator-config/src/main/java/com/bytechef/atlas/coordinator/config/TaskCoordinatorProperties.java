/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.coordinator.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Arik Cohen
 */
@ConfigurationProperties(prefix = "bytechef.coordinator.task")
@SuppressFBWarnings("EI")
public class TaskCoordinatorProperties {

    private TaskCoordinatorSubscriptions subscriptions = new TaskCoordinatorSubscriptions();

    public TaskCoordinatorSubscriptions getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(TaskCoordinatorSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    public static class TaskCoordinatorSubscriptions {

        private int applicationEvents = 1;
        private int resumeJobEvents = 1;
        private int startJobEvents = 1;
        private int stopJobEvents = 1;
        private int taskExecutionCompleteEvents = 1;
        private int taskExecutionErrorEvents = 1;

        public int getApplicationEvents() {
            return applicationEvents;
        }

        public int getResumeJobEvents() {
            return resumeJobEvents;
        }

        public int getStartJobEvents() {
            return startJobEvents;
        }

        public int getStopJobEvents() {
            return stopJobEvents;
        }

        public int getTaskExecutionCompleteEvents() {
            return taskExecutionCompleteEvents;
        }

        public int getTaskExecutionErrorEvents() {
            return taskExecutionErrorEvents;
        }

        public void setApplicationEvents(int applicationEvents) {
            this.applicationEvents = applicationEvents;
        }

        public void setResumeJobEvents(int resumeJobEvents) {
            this.resumeJobEvents = resumeJobEvents;
        }

        public void setStartJobEvents(int startJobEvents) {
            this.startJobEvents = startJobEvents;
        }

        public void setStopJobEvents(int stopJobEvents) {
            this.stopJobEvents = stopJobEvents;
        }

        public void setTaskExecutionCompleteEvents(int taskExecutionCompleteEvents) {
            this.taskExecutionCompleteEvents = taskExecutionCompleteEvents;
        }

        public void setTaskExecutionErrorEvents(int taskExecutionErrorEvents) {
            this.taskExecutionErrorEvents = taskExecutionErrorEvents;
        }
    }
}
