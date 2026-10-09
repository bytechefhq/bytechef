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

package com.bytechef.component.ai.agent.chat.memory.aws.util;

import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.BUCKET;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.REGION;
import static com.bytechef.component.ai.agent.chat.memory.aws.constant.AwsChatMemoryConstants.SECRET_ACCESS_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.session.SessionRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * @author Ivica Cardic
 */
class AwsSessionChatMemoryUtilsTest {

    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final AtomicLong tickerNanos = new AtomicLong();

    private final Scheduler manualScheduler = (executor, command, delay, timeUnit) -> {
        scheduledTasks.add(() -> executor.execute(command));

        return new CompletableFuture<>();
    };

    @Test
    void testSameConnectionReusesOneS3Client() {
        S3Client firstS3Client = AwsSessionChatMemoryUtils.getSharedS3Client(connectionParameters("secret-a"));
        S3Client secondS3Client = AwsSessionChatMemoryUtils.getSharedS3Client(connectionParameters("secret-a"));

        assertThat(secondS3Client).isSameAs(firstS3Client);
    }

    @Test
    void testDifferentCredentialsGetDifferentS3Clients() {
        S3Client firstS3Client = AwsSessionChatMemoryUtils.getSharedS3Client(connectionParameters("secret-a"));
        S3Client secondS3Client = AwsSessionChatMemoryUtils.getSharedS3Client(connectionParameters("secret-b"));

        assertThat(secondS3Client).isNotSameAs(firstS3Client);
    }

    @Test
    void testKeyPrefixDefaultsToSessionFolder() {
        S3Client s3Client = mock(S3Client.class);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build());

        SessionRepository sessionRepository = AwsSessionChatMemoryUtils.createSessionRepository(
            s3Client, MockParametersFactory.create(Map.of(BUCKET, "user-bucket")));

        sessionRepository.findById("session-1");

        ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);

        verify(s3Client).getObjectAsBytes(getObjectRequestCaptor.capture());

        GetObjectRequest getObjectRequest = getObjectRequestCaptor.getValue();

        assertThat(getObjectRequest.key()).isEqualTo("session/session-1.json");
    }

    @Test
    void testConfiguredKeyPrefixIsUsed() {
        S3Client s3Client = mock(S3Client.class);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build());

        SessionRepository sessionRepository = AwsSessionChatMemoryUtils.createSessionRepository(
            s3Client, MockParametersFactory.create(Map.of(BUCKET, "user-bucket", KEY_PREFIX, "memory/")));

        sessionRepository.findById("session-1");

        ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);

        verify(s3Client).getObjectAsBytes(getObjectRequestCaptor.capture());

        GetObjectRequest getObjectRequest = getObjectRequestCaptor.getValue();

        assertThat(getObjectRequest.key()).isEqualTo("memory/session/session-1.json");
    }

    @Test
    void testIdleClientIsClosedWhenTheSchedulerExpiresIt() {
        Cache<String, S3Client> s3Clients = AwsSessionChatMemoryUtils.createClientCache(clientCacheSettings());

        S3Client idleS3Client = mock(S3Client.class);

        s3Clients.put("idle", idleS3Client);

        Duration expiredIdleTime = ClientCacheSettings.IDLE_TIMEOUT.plusSeconds(1);

        tickerNanos.addAndGet(expiredIdleTime.toNanos());

        runTasks(scheduledTasks);

        assertThat(s3Clients.asMap()).isEmpty();

        verify(idleS3Client).close();
    }

    @Test
    void testSizeEvictedClientIsClosedOnlyAfterTheGracePeriod() {
        Cache<String, S3Client> s3Clients = AwsSessionChatMemoryUtils.createClientCache(clientCacheSettings());

        S3Client firstS3Client = mock(S3Client.class);
        S3Client secondS3Client = mock(S3Client.class);

        s3Clients.put("first", firstS3Client);
        s3Clients.put("second", secondS3Client);

        s3Clients.cleanUp();

        Map<String, S3Client> remainingS3Clients = s3Clients.asMap();

        assertThat(remainingS3Clients).hasSize(1);

        S3Client evictedS3Client = remainingS3Clients.containsValue(firstS3Client) ? secondS3Client : firstS3Client;
        S3Client keptS3Client = evictedS3Client == firstS3Client ? secondS3Client : firstS3Client;

        verify(evictedS3Client, never()).close();

        runTasks(deferredCloseTasks);

        verify(evictedS3Client).close();
        verify(keptS3Client, never()).close();
    }

    private static Parameters connectionParameters(String secretAccessKey) {
        return MockParametersFactory.create(
            Map.of(REGION, "us-east-1", ACCESS_KEY_ID, "access-key", SECRET_ACCESS_KEY, secretAccessKey));
    }

    private ClientCacheSettings clientCacheSettings() {
        return new ClientCacheSettings(1, tickerNanos::get, manualScheduler, Runnable::run, deferredCloseTasks::add);
    }

    private static void runTasks(List<Runnable> tasks) {
        List<Runnable> pendingTasks = new ArrayList<>(tasks);

        tasks.clear();

        pendingTasks.forEach(Runnable::run);
    }

    @Test
    void testSessionDocumentsLiveUnderTheSessionSubPrefixOfTheConnectionKeyPrefix() {
        assertThat(
            AwsSessionChatMemoryUtils.getSessionKeyPrefix(MockParametersFactory.create(Map.of("bucket", "memory"))))
                .isEqualTo("session/");
        assertThat(
            AwsSessionChatMemoryUtils.getSessionKeyPrefix(
                MockParametersFactory.create(Map.of("bucket", "memory", "keyPrefix", "acme/"))))
                    .isEqualTo("acme/session/");
    }
}
