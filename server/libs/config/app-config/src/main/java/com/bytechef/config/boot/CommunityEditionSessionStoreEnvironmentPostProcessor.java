/*
 * Copyright 2025 ByteChef
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
 */

package com.bytechef.config.boot;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Logs that the Redis and JDBC HTTP session stores are Enterprise Edition features when one of them is configured in
 * the Community Edition, which always keeps sessions in memory.
 *
 * @author Ivica Cardic
 */
public class CommunityEditionSessionStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final Log log;

    public CommunityEditionSessionStoreEnvironmentPostProcessor(DeferredLogFactory deferredLogFactory) {
        this.log = deferredLogFactory.getLog(CommunityEditionSessionStoreEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String edition = environment.getProperty("bytechef.edition");

        if (edition != null && !"ce".equalsIgnoreCase(edition)) {
            return;
        }

        String store = environment.getProperty("bytechef.session.store");

        if ("redis".equalsIgnoreCase(store) || "jdbc".equalsIgnoreCase(store)) {
            log.info(
                "bytechef.session.store=" + store + " requires ByteChef Enterprise Edition; using memory sessions");
        }
    }
}
