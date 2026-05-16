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

package com.bytechef.platform.ai.stt.web.rest;

import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import com.bytechef.platform.ai.stt.service.TranscribeService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Push-to-talk transcription endpoint. Accepts a recorded audio clip and returns its transcript, using the configured
 * speech-to-text provider. Served under {@code /api/platform/internal}, so it is available only to authenticated
 * application users.
 *
 * @author Ivica Cardic
 */
@RestController
class TranscribeController {

    private final TranscribeService transcribeService;

    @SuppressFBWarnings("EI2")
    TranscribeController(TranscribeService transcribeService) {
        this.transcribeService = transcribeService;
    }

    @PostMapping(value = "/api/platform/internal/ai/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscribeResponse transcribe(
        @RequestParam("file") MultipartFile file, @RequestParam(value = "locale", required = false) String locale)
        throws IOException {

        TranscriptResult result = transcribeService.transcribe(file.getInputStream(), file.getContentType(), locale);

        return new TranscribeResponse(result.text(), result.durationMs(), result.detectedLocale());
    }

    record TranscribeResponse(String text, long durationMs, String detectedLocale) {
    }
}
