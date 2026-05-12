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

package com.bytechef.automation.assetfile.metric;

import com.bytechef.automation.assetfile.domain.AssetFileSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AssetFileMetrics {

    private static final String COUNTER_NAME = "bytechef_asset_file_create";
    private static final String BLOB_ORPHAN_COUNTER_NAME = "bytechef_asset_file_blob_orphan_total";
    private static final String STREAM_FAILURE_COUNTER_NAME = "bytechef_asset_file_stream_failure_total";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public AssetFileMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    public void recordCreate(AssetFileSource source, String mimeType) {
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

    /**
     * Records a leaked blob: the database row for a deleted asset file was committed but the underlying file-storage
     * delete failed, leaving an orphaned object. Without this counter, ops have no visibility into the leak — workspace
     * quotas eventually reject new uploads with no clear cause. Tag {@code reason} categorises the failure mode at a
     * coarse level (the full exception is still logged at WARN by the caller).
     */
    public void recordBlobOrphan(String reason) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(BLOB_ORPHAN_COUNTER_NAME)
            .tag("reason", reason == null ? "unknown" : reason)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records that an asset-file content download failed mid-stream after the response status was already committed.
     * Without this counter, an {@code IOException} thrown from {@code transferTo(...)} surfaces only as a generic
     * Spring stack with no asset-file-level signal; ops have no way to distinguish a flaky storage backend from a
     * client disconnect. Tag {@code reason} carries the simple exception class name (the full stack is logged WARN by
     * the caller).
     */
    public void recordStreamFailure(String reason) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder(STREAM_FAILURE_COUNTER_NAME)
            .tag("reason", reason == null ? "unknown" : reason)
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
