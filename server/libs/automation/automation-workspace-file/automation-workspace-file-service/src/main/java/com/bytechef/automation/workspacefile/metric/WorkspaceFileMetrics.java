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

package com.bytechef.automation.workspacefile.metric;

import com.bytechef.automation.workspacefile.domain.WorkspaceFileSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkspaceFileMetrics {

    private static final String COUNTER_NAME = "bytechef_workspace_file_create";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WorkspaceFileMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    public void recordCreate(WorkspaceFileSource source, String mimeType) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(COUNTER_NAME)
            .tag("source", source.name())
            .tag("mime_type_category", categorize(mimeType))
            .register(meterRegistry)
            .increment();
    }

    private String categorize(String mimeType) {
        if (mimeType == null) {
            return "other";
        }

        if (mimeType.startsWith("text/") || "application/json".equals(mimeType)) {
            return "text";
        }

        if (mimeType.startsWith("image/")) {
            return "image";
        }

        if ("application/pdf".equals(mimeType)) {
            return "pdf";
        }

        return "other";
    }
}
