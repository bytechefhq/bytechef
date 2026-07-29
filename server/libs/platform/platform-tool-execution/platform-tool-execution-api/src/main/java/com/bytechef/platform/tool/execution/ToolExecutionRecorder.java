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

package com.bytechef.platform.tool.execution;

import java.util.function.Supplier;

/**
 * Entry point every direct tool/action execution call site uses to record its invocation. Recording is fire-and-forget
 * telemetry: it never alters the wrapped execution's return value or the exception it throws.
 *
 * @author Ivica Cardic
 */
public interface ToolExecutionRecorder {

    /**
     * Times and runs {@code execution}, then emits a {@link ToolExecutionEvent} built from {@code builder} with the
     * measured duration and the resolved outcome ({@code SUCCESS}, or {@code TIMEOUT} / {@code ERROR} when the
     * execution throws). The original return value is returned and any thrown exception is rethrown unchanged.
     */
    <V> V record(ToolExecutionEvent.Builder builder, Supplier<V> execution);

    /**
     * Emits a fully-built event directly, for terminal outcomes that are not produced by running an execution (e.g.
     * {@code CONNECTION_REQUIRED}).
     */
    void record(ToolExecutionEvent event);
}
