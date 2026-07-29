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

package com.bytechef.component.definition.datastream;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.Map;

/**
 * Reads items one at a time from a SOURCE component during a data stream job. Combines the lifecycle callbacks of
 * {@link ItemStream} with the schema discovery of {@link FieldsProvider}, and may optionally support incremental
 * syncing driven by the {@link #SINCE_KEY} timestamp stored in the {@link ExecutionContext}.
 *
 * @author Ivica Cardic
 */
public interface ItemReader extends ItemStream, FieldsProvider {

    /**
     * Cluster element type identifying a reader as the SOURCE endpoint of a data stream.
     */
    ClusterElementType SOURCE = new ClusterElementType("SOURCE", "source", "Source");

    /**
     * Well-known {@link ExecutionContext} key for the previous successful run's start instant, stored as a {@code Long}
     * epoch-millisecond value.
     *
     * <p>
     * Readers that opt into incremental sync (return {@code true} from {@link #supportsIncremental()}) read this key in
     * their {@code open()} method:
     *
     * <pre>{@code
     * Long sinceMillis = executionContext.get(SINCE_KEY, Long.class)
     *     .orElse(null);
     * Instant since = sinceMillis == null ? null : Instant.ofEpochMilli(sinceMillis);
     * }</pre>
     *
     * <p>
     * When absent, the reader should perform a full pull (the contract for "first run" or "no prior state available").
     * The {@code datastream.since} JobParameter, when set on the launching {@code JobLauncher.run(...)} call, is copied
     * into the per-step {@code ExecutionContext} under this key by {@code ItemStreamReaderDelegate.doBeforeStep}.
     *
     * @since the Phase 17 incremental-sync SPI extension
     */
    String SINCE_KEY = "datastream.since";

    /**
     * Whether this reader can filter upstream by a {@link #SINCE_KEY since} timestamp passed via the
     * {@link ExecutionContext}. Default {@code false} keeps every existing reader at full-pull behavior.
     *
     * <p>
     * Returning {@code true} signals to the orchestrator (e.g., the Phase 17b workflow generator) that it can safely
     * inject the {@code datastream.since} JobParameter on subsequent runs to enable incremental syncing. Returning
     * {@code true} but ignoring {@link #SINCE_KEY} in {@code open()} is a contract violation.
     *
     * @return {@code true} if this reader honors {@link #SINCE_KEY} for incremental filtering
     * @since the Phase 17 incremental-sync SPI extension
     */
    default boolean supportsIncremental() {
        return false;
    }

    /**
     * Reads and returns the next item from the source, or {@code null} when the source is exhausted. Each item is
     * represented as a map keyed by field name.
     *
     * @return the next item as a map of field name to value, or {@code null} if no more items are available
     * @throws Exception if reading the next item fails
     */
    Map<String, Object> read() throws Exception;
}
