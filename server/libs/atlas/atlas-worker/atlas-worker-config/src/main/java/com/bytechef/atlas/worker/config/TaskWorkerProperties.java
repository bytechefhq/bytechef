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

package com.bytechef.atlas.worker.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Arik Cohen
 */
@ConfigurationProperties(prefix = "bytechef.worker.task")
@SuppressFBWarnings("EI")
public class TaskWorkerProperties {

    private Map<String, Object> subscriptions = new HashMap<>();

    public Map<String, Object> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Map<String, Object> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
