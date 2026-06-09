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

package org.springframework.ai.session.s3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.SessionEvent;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mocked unit tests for the CAS (compare-and-swap) paths of
 * {@link S3SessionRepository#replaceEvents(String, List, long)}.
 *
 * <p>
 * LocalStack 3.5 does not enforce S3 {@code If-Match} conditional writes, so the 412-PreconditionFailed branch cannot
 * be exercised by the integration test. These tests mock the S3Client directly for deterministic coverage.
 *
 * @author Ivica Cardic
 */
class S3SessionRepositoryCasTest {

    private static final String SESSION_ID = "sess";
    private static final String BUCKET = "b";
    private static final String ETAG = "\"etag-v5\"";
    private static final long VERSION = 5L;

    private S3Client s3Client;
    private S3SessionRepository repository;
    private byte[] storedSessionBytes;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);

        StoredSession storedSession = new StoredSession(
            SESSION_ID, "user-1", System.currentTimeMillis(), null, Map.of(), VERSION, List.of());

        storedSessionBytes = JsonMapper.builder()
            .build()
            .writeValueAsBytes(storedSession);

        repository = S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(BUCKET)
            .build();
    }

    @Test
    void testReplaceEventsReturnsFalseWhenPutThrows412() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder()
                .statusCode(412)
                .message("PreconditionFailed")
                .build());

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        boolean result = repository.replaceEvents(SESSION_ID, events, VERSION);

        assertFalse(result);
    }

    @Test
    void testReplaceEventsReturnsTrueWhenPutSucceeds() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        boolean result = repository.replaceEvents(SESSION_ID, events, VERSION);

        assertTrue(result);
    }

    @Test
    void testReplaceEventsReturnsFalseOnStaleVersionWithoutInvokingPut() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        long staleVersion = VERSION - 1L;

        boolean result = repository.replaceEvents(SESSION_ID, events, staleVersion);

        assertFalse(result);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
