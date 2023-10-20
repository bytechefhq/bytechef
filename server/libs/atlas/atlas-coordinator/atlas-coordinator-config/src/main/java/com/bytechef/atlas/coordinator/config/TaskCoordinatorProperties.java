
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
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

        private int completions = 1;
        private int jobs = 1;
        private int restarts = 1;
        private int stops = 1;
        private int subflows = 1;

        public int getCompletions() {
            return completions;
        }

        public int getJobs() {
            return jobs;
        }

        public int getRestarts() {
            return restarts;
        }

        public int getStops() {
            return stops;
        }

        public int getSubflows() {
            return subflows;
        }

        public void setCompletions(int completions) {
            this.completions = completions;
        }

        public void setJobs(int jobs) {
            this.jobs = jobs;
        }

        public void setRestarts(int restarts) {
            this.restarts = restarts;
        }

        public void setStops(int stops) {
            this.stops = stops;
        }

        public void setSubflows(int subflows) {
            this.subflows = subflows;
        }
    }
}
