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

package com.bytechef.platform.ai.stt.service;

import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;

/**
 * Transcribes an audio clip to text using the configured speech-to-text provider. Backs the push-to-talk transcribe
 * endpoint.
 *
 * @author Ivica Cardic
 */
public interface TranscribeService {

    /**
     * Transcribes the given audio.
     *
     * @param audio    the audio clip to transcribe
     * @param mimeType the audio MIME type (e.g. {@code audio/webm})
     * @param locale   an optional BCP-47 locale hint, or {@code null} to auto-detect
     * @return the transcription result
     */
    TranscriptResult transcribe(InputStream audio, @Nullable String mimeType, @Nullable String locale);
}
