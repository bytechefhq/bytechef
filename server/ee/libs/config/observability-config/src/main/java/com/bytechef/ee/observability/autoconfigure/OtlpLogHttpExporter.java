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

package com.bytechef.ee.observability.autoconfigure;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collection;

public class OtlpLogHttpExporter implements LogRecordExporter {

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        // Left out code for REUSABLE_DATA from LogReusableDataMarshaler
        // MemoryMode == MemoryMode.IMMUTABLE_DATA
        // see SDK explanation - see if we can use protoBuf encoder

        return new CompletableResultCode();
    }

    @Override
    public CompletableResultCode flush() {
        return null;
    }

    @Override
    public CompletableResultCode shutdown() {
        return null;
    }
}
