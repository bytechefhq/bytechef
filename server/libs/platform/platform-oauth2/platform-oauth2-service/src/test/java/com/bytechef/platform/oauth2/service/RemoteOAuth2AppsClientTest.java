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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.githubproxy.client.OAuthAppProxyClient;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("unchecked")
class RemoteOAuth2AppsClientTest {

    private static final String REJECTED_KEY_WARNING = "the proxy rejected the API key (HTTP 401)";

    private final ObjectProvider<OAuthAppProxyClient> objectProvider = mock(ObjectProvider.class);
    private final OAuthAppProxyClient oAuthAppProxyClient = mock(OAuthAppProxyClient.class);
    private final MutableClock clock = new MutableClock();

    @BeforeEach
    void beforeEach() {
        when(objectProvider.getIfAvailable()).thenReturn(oAuthAppProxyClient);
    }

    @Test
    void testNoClientMeansEmpty() {
        when(objectProvider.getIfAvailable()).thenReturn(null);

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).isEmpty();
        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).isEmpty();

        verify(objectProvider, times(1)).getIfAvailable();
    }

    @Test
    void testKeepsValidAppsAndNamesIgnoredEntriesInWarning(CapturedOutput output) {
        Map<String, OAuthApp> oAuthApps = new HashMap<>();

        oAuthApps.put("googleDrive", new OAuthApp("id", "s3cr3t"));
        oAuthApps.put("blankId", new OAuthApp(" ", "s3cr3t"));
        oAuthApps.put("nullSecret", new OAuthApp("id", null));
        oAuthApps.put("nullApp", null);

        when(oAuthAppProxyClient.getOAuthApps()).thenReturn(oAuthApps);

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        Map<String, OAuthApp> apps = remoteOAuth2AppsClient.getOAuth2Apps();

        assertThat(apps).containsExactly(Map.entry("googleDrive", new OAuthApp("id", "s3cr3t")));
        assertThat(output).contains("ignored entries without a clientId or clientSecret: blankId, nullApp, nullSecret");
        assertThat(output).doesNotContain("s3cr3t");
    }

    @Test
    void testCachesForFiveMinutes() {
        when(oAuthAppProxyClient.getOAuthApps()).thenReturn(Map.of("a", new OAuthApp("id", "secret")));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(299);

        remoteOAuth2AppsClient.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(1)).getOAuthApps();

        clock.advanceSeconds(1);

        remoteOAuth2AppsClient.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();
    }

    @Test
    void testFailureKeepsLastGoodCopy() {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(301);

        Map<String, OAuthApp> apps = remoteOAuth2AppsClient.getOAuth2Apps();

        assertThat(apps).containsOnlyKeys("a");

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();
    }

    @Test
    void testResponseWithoutBodyKeepsLastGoodCopyAndWarns(CapturedOutput output) {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenThrow(responseException(HttpStatus.NO_CONTENT))
            .thenThrow(responseException(HttpStatus.FOUND));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(301);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).containsOnlyKeys("a");
        assertThat(output).contains("the proxy answered without a body (HTTP 204)");

        clock.advanceSeconds(61);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).containsOnlyKeys("a");
        assertThat(output).contains("the proxy answered with a redirect (HTTP 302), check github-proxy.base-url");
    }

    @Test
    void testEmptyMapReplacesLastGoodCopy() {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenReturn(Map.of());

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(301);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).isEmpty();
    }

    @Test
    void testFailureWithoutCopyReturnsEmpty() {
        when(oAuthAppProxyClient.getOAuthApps()).thenThrow(new IllegalStateException("boom"));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).isEmpty();
    }

    @Test
    void testFailureRetryWaitsSixtySeconds() {
        when(oAuthAppProxyClient.getOAuthApps()).thenThrow(unauthorized());

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(30);

        remoteOAuth2AppsClient.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(1)).getOAuthApps();

        clock.advanceSeconds(31);

        remoteOAuth2AppsClient.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();
    }

    @Test
    void testFailureAfterSuccessRetryWaitsSixtySeconds() {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenThrow(unauthorized());

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(301);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(59);

        remoteOAuth2AppsClient.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(2)).getOAuthApps();

        clock.advanceSeconds(1);

        remoteOAuth2AppsClient.getOAuth2Apps();

        verify(oAuthAppProxyClient, times(3)).getOAuthApps();
    }

    @Test
    void testRejectedKeyWarnsWithPropertyHint(CapturedOutput output) {
        when(oAuthAppProxyClient.getOAuthApps()).thenThrow(unauthorized());

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        assertThat(output).contains(REJECTED_KEY_WARNING + ", check github-proxy.oauth-apps.api-key");
    }

    @Test
    void testSameWarningIsRateLimitedToOncePerHour(CapturedOutput output) {
        when(oAuthAppProxyClient.getOAuthApps()).thenThrow(unauthorized());

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(61);

        remoteOAuth2AppsClient.getOAuth2Apps();

        assertThat(StringUtils.countMatches(output.getOut(), REJECTED_KEY_WARNING)).isEqualTo(1);

        clock.advanceSeconds(3600);

        remoteOAuth2AppsClient.getOAuth2Apps();

        assertThat(StringUtils.countMatches(output.getOut(), REJECTED_KEY_WARNING)).isEqualTo(2);
    }

    @Test
    void testDifferentFailureWarnsWithinTheHour(CapturedOutput output) {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenThrow(new ResourceAccessException("timeout"))
            .thenThrow(unauthorized());

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(61);

        remoteOAuth2AppsClient.getOAuth2Apps();

        assertThat(output).contains("the request to the proxy failed: ResourceAccessException");
        assertThat(output).contains(REJECTED_KEY_WARNING);
        assertThat(output).doesNotContain("timeout");
    }

    @Test
    void testRecoveryIsLogged(CapturedOutput output) {
        when(oAuthAppProxyClient.getOAuthApps())
            .thenThrow(unauthorized())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(61);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).containsOnlyKeys("a");
        assertThat(output).contains("Loaded 1 predefined OAuth apps from the GitHub proxy again");
    }

    @Test
    void testStaleCopyIsServedWhileRefreshIsInFlight() throws InterruptedException {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);

        when(oAuthAppProxyClient.getOAuthApps())
            .thenReturn(Map.of("a", new OAuthApp("id", "secret")))
            .thenAnswer(blockingFetch(fetchStarted, releaseFetch, Map.of("b", new OAuthApp("id2", "secret2"))));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(301);

        assertConcurrentCallerNotBlocked(remoteOAuth2AppsClient, fetchStarted, releaseFetch, "a", "b");
    }

    @Test
    void testEmptyCopyIsServedWhileRetryIsInFlightAfterFailure() throws InterruptedException {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);

        when(oAuthAppProxyClient.getOAuthApps())
            .thenThrow(unauthorized())
            .thenAnswer(blockingFetch(fetchStarted, releaseFetch, Map.of("b", new OAuthApp("id2", "secret2"))));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        remoteOAuth2AppsClient.getOAuth2Apps();

        clock.advanceSeconds(61);

        assertConcurrentCallerNotBlocked(remoteOAuth2AppsClient, fetchStarted, releaseFetch, null, "b");
    }

    @Test
    void testProviderFailureIsTreatedAsDisabledForThatCall(CapturedOutput output) {
        when(objectProvider.getIfAvailable()).thenThrow(new IllegalStateException("boom"))
            .thenReturn(oAuthAppProxyClient);
        when(oAuthAppProxyClient.getOAuthApps()).thenReturn(Map.of("a", new OAuthApp("id", "secret")));

        RemoteOAuth2AppsClient remoteOAuth2AppsClient = new RemoteOAuth2AppsClient(objectProvider, clock);

        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).isEmpty();
        assertThat(output).contains("the OAuth apps proxy client could not be created: IllegalStateException");
        assertThat(remoteOAuth2AppsClient.getOAuth2Apps()).containsOnlyKeys("a");
    }

    private static Answer<Map<String, OAuthApp>> blockingFetch(
        CountDownLatch fetchStarted, CountDownLatch releaseFetch, Map<String, OAuthApp> result) {

        return invocation -> {
            fetchStarted.countDown();

            if (!releaseFetch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("not released");
            }

            return result;
        };
    }

    private static void assertConcurrentCallerNotBlocked(
        RemoteOAuth2AppsClient remoteOAuth2AppsClient, CountDownLatch fetchStarted, CountDownLatch releaseFetch,
        String expectedConcurrentKey, String expectedRefreshedKey) throws InterruptedException {

        AtomicReference<Map<String, OAuthApp>> refreshingResult = new AtomicReference<>();
        AtomicReference<Map<String, OAuthApp>> concurrentResult = new AtomicReference<>();

        Thread refreshingThread = new Thread(() -> refreshingResult.set(remoteOAuth2AppsClient.getOAuth2Apps()));
        Thread concurrentThread = new Thread(() -> concurrentResult.set(remoteOAuth2AppsClient.getOAuth2Apps()));

        try {
            refreshingThread.start();

            assertThat(fetchStarted.await(10, TimeUnit.SECONDS)).isTrue();

            concurrentThread.start();
            concurrentThread.join(5000);

            assertThat(concurrentThread.isAlive()).isFalse();

            if (expectedConcurrentKey == null) {
                assertThat(concurrentResult.get()).isEmpty();
            } else {
                assertThat(concurrentResult.get()).containsOnlyKeys(expectedConcurrentKey);
            }
        } finally {
            releaseFetch.countDown();
        }

        refreshingThread.join(10000);

        assertThat(refreshingThread.isAlive()).isFalse();
        assertThat(refreshingResult.get()).containsOnlyKeys(expectedRefreshedKey);
    }

    private static RestClientResponseException responseException(HttpStatus httpStatus) {
        return new RestClientResponseException("ignored", httpStatus, "", null, null, null);
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
