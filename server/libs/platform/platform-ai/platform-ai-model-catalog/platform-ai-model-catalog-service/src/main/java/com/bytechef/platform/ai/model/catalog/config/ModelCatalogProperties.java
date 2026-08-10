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

package com.bytechef.platform.ai.model.catalog.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai.model-catalog")
@SuppressFBWarnings("EI")
public class ModelCatalogProperties {

    private Refresh refresh = new Refresh();

    public Refresh getRefresh() {
        return refresh;
    }

    public void setRefresh(Refresh refresh) {
        this.refresh = refresh;
    }

    public static class Refresh {

        private boolean enabled = true;
        private String url = "https://models.dev/api.json";

        // Deliberately no `interval` property here. bytechef.ai.model-catalog.refresh.interval is consumed directly
        // by ModelsDevRefresher's @Scheduled placeholder (ISO-8601 duration, e.g. P1D), never through this class. A
        // Duration-typed property here previously coexisted with that raw placeholder read, accepting Spring's
        // relaxed-binding formats (e.g. "1d") that bind fine but then fail @Scheduled's stricter ISO-8601 parsing at
        // context startup — two incompatible formats for what operators would reasonably assume is one setting.

        public String getUrl() {
            return url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
