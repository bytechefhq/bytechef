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

package com.bytechef.platform.workflow.coordinator.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Arik Cohen
 */
@ConfigurationProperties(prefix = "bytechef.coordinator.trigger")
@SuppressFBWarnings("EI")
public class TriggerCoordinatorProperties {

    private TriggerCoordinatorSubscriptions subscriptions = new TriggerCoordinatorSubscriptions();

    public TriggerCoordinatorSubscriptions getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(TriggerCoordinatorSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    public static class TriggerCoordinatorSubscriptions {

        private int applicationEvents = 1;
        private int triggerExecutionCompleteEvents = 1;
        private int triggerExecutionErrorEvents = 1;
        private int triggerListenerEvents = 1;
        private int triggerPollEvents = 1;
        private int triggerWebhookEvents = 1;

        public int getApplicationEvents() {
            return applicationEvents;
        }

        public int getTriggerExecutionCompleteEvents() {
            return triggerExecutionCompleteEvents;
        }

        public int getTriggerExecutionErrorEvents() {
            return triggerExecutionErrorEvents;
        }

        public int getTriggerListenerEvents() {
            return triggerListenerEvents;
        }

        public int getTriggerPollEvents() {
            return triggerPollEvents;
        }

        public int getTriggerWebhookEvents() {
            return triggerWebhookEvents;
        }

        public void setApplicationEvents(int applicationEvents) {
            this.applicationEvents = applicationEvents;
        }

        public void setTriggerExecutionCompleteEvents(int triggerExecutionCompleteEvents) {
            this.triggerExecutionCompleteEvents = triggerExecutionCompleteEvents;
        }

        public void setTriggerExecutionErrorEvents(int triggerExecutionErrorEvents) {
            this.triggerExecutionErrorEvents = triggerExecutionErrorEvents;
        }

        public void setTriggerListenerEvents(int triggerListenerEvents) {
            this.triggerListenerEvents = triggerListenerEvents;
        }

        public void setTriggerPollEvents(int triggerPollEvents) {
            this.triggerPollEvents = triggerPollEvents;
        }

        public void setTriggerWebhookEvents(int triggerWebhookEvents) {
            this.triggerWebhookEvents = triggerWebhookEvents;
        }
    }
}
