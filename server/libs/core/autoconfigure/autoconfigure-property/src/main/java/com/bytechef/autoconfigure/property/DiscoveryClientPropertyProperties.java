/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.autoconfigure.property;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings({"EI", "EI2"})
@ConfigurationProperties(prefix = "discovery-client")
public class DiscoveryClientPropertyProperties {

    private Map<String, Application> property = new HashMap<>();

    public Map<String, Application> getProperty() {
        return property;
    }

    public void setProperty(Map<String, Application> property) {
        this.property = property;
    }

    public static class Application {
        private List<Instance> instances = Collections.emptyList();

        public List<Instance> getInstances() {
            return instances;
        }

        public void setInstances(List<Instance> instances) {
            this.instances = instances;
        }
    }

    public static class Instance {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
