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

package com.bytechef.platform.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties.Oauth2.OAuth2App;
import com.bytechef.platform.githubproxy.client.OAuthAppProxyClient;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class RemoteOAuth2AppsTest {

    private final ObjectProvider<OAuthAppProxyClient> objectProvider = mock(ObjectProvider.class);
    private final OAuthAppProxyClient oAuthAppProxyClient = mock(OAuthAppProxyClient.class);
    private final MutableClock clock = new MutableClock();

    @BeforeEach
    void beforeEach() {
        when(objectProvider.getIfAvailable()).thenReturn(oAuthAppProxyClient);
    }

    @Test
    void testNoClientMeansEmptyAndNoCall() {
        when(objectProvider.getIfAvailable()).thenReturn(null);

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        assertThat(remoteOAuth2Apps.getOAuth2Apps()).isEmpty();

        verify(oAuthAppProxyClient, never()).getOAuthApps();
    }

    @Test
    void testConvertsAppsAndSkipsBlankEntries() {
        when(oAuthAppProxyClient.getOAuthApps()).thenReturn(
            Map.of(
                "googleDrive", new OAuthApp("id", "secret"),
                "blankId", new OAuthApp(" ", "secret"),
                "nullSecret", new OAuthApp("id", null)));

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        Map<String, OAuth2App> apps = remoteOAuth2Apps.getOAuth2Apps();

        assertThat(apps).containsOnlyKeys("googleDrive");

        OAuth2App oAuth2App = apps.get("googleDrive");

        assertThat(oAuth2App.getClientId()).isEqualTo("id");
        assertThat(oAuth2App.getClientSecret()).isEqualTo("secret");
    }

    @Test
    void testCachesForFiveMinutes() {
        when(oAuthAppProxyClient.getOAuthApps()).thenReturn(Map.of("a", new OAuthApp("id", "secret")));

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        remoteOAuth2Apps.getOAuth2Apps();

        clock.advanceSeconds(299);

        remoteOAuth2Apps.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(1)).getOAuthApps();

        clock.advanceSeconds(1);

        remoteOAuth2Apps.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();
    }

    @Test
    void testFailureKeepsLastGoodCopy() {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenThrow(unauthorized());

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        remoteOAuth2Apps.getOAuth2Apps();

        clock.advanceSeconds(301);

        Map<String, OAuth2App> apps = remoteOAuth2Apps.getOAuth2Apps();

        assertThat(apps).containsOnlyKeys("a");

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();
    }

    @Test
    void testFailureWithoutCopyReturnsEmpty() {
        when(oAuthAppProxyClient.getOAuthApps()).thenThrow(new IllegalStateException("boom"));

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        assertThat(remoteOAuth2Apps.getOAuth2Apps()).isEmpty();
    }

    @Test
    void testFailureRetryWaitsSixtySeconds() {
        when(oAuthAppProxyClient.getOAuthApps()).thenThrow(unauthorized());

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        remoteOAuth2Apps.getOAuth2Apps();

        clock.advanceSeconds(30);

        remoteOAuth2Apps.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(1)).getOAuthApps();

        clock.advanceSeconds(31);

        remoteOAuth2Apps.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();
    }

    @Test
    void testStaleCopyIsServedWhileRefreshIsInFlight() throws InterruptedException {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        AtomicReference<Map<String, OAuth2App>> refreshingResult = new AtomicReference<>();

        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenAnswer(
                invocation -> {
                    fetchStarted.countDown();

                    if (!releaseFetch.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("not released");
                    }

                    return Map.of("b", new OAuthApp("id2", "secret2"));
                });

        RemoteOAuth2Apps remoteOAuth2Apps = new RemoteOAuth2Apps(objectProvider, clock);

        remoteOAuth2Apps.getOAuth2Apps();

        clock.advanceSeconds(301);

        Thread refreshingThread = new Thread(() -> refreshingResult.set(remoteOAuth2Apps.getOAuth2Apps()));

        refreshingThread.start();

        assertThat(fetchStarted.await(10, TimeUnit.SECONDS)).isTrue();

        AtomicReference<Map<String, OAuth2App>> concurrentResult = new AtomicReference<>();
        Thread concurrentThread = new Thread(() -> concurrentResult.set(remoteOAuth2Apps.getOAuth2Apps()));

        concurrentThread.start();
        concurrentThread.join(5000);

        assertThat(concurrentThread.isAlive()).isFalse();
        assertThat(concurrentResult.get()).containsOnlyKeys("a");

        releaseFetch.countDown();

        refreshingThread.join(10000);

        assertThat(refreshingThread.isAlive()).isFalse();
        assertThat(refreshingResult.get()).containsOnlyKeys("b");
    }

    private static HttpClientErrorException unauthorized() {
        return HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
