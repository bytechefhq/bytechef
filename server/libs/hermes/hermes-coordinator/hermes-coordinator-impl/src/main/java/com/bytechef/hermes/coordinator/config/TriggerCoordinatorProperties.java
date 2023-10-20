
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

package com.bytechef.hermes.coordinator.config;

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

        private int listeners = 1;
        private int polls = 1;
        private int triggersComplete = 1;
        private int webhooks = 1;

        public int getListeners() {
            return listeners;
        }

        public int getPolls() {
            return polls;
        }

        public int getTriggersComplete() {
            return triggersComplete;
        }

        public int getWebhooks() {
            return webhooks;
        }

        public void setListeners(int listeners) {
            this.listeners = listeners;
        }

        public void setPolls(int polls) {
            this.polls = polls;
        }

        public void setTriggersComplete(int triggersComplete) {
            this.triggersComplete = triggersComplete;
        }

        public void setWebhooks(int webhooks) {
            this.webhooks = webhooks;
        }
    }
}
