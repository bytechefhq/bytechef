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

package com.bytechef.platform.ai.stt;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.Map;

public interface SttProvider {

    String getKey();

    TranscriptResult transcribe(TranscribeRequest request);

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record TranscribeRequest(
        InputStream audio,
        String mimeType,
        String locale,
        Map<String, Object> connectionParameters) {
    }

    record TranscriptResult(
        String text,
        long durationMs,
        String detectedLocale) {
    }
}
