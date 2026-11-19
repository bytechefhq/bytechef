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

import com.bytechef.platform.githubproxy.client.OAuthAppProxyClient;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteOAuth2AppsClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteOAuth2AppsClient.class);

    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(60);
    private static final Duration WARN_INTERVAL = Duration.ofHours(1);

    private final Clock clock;
    private final Map<String, Instant> lastWarnAtByReason = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ObjectProvider<OAuthAppProxyClient> oAuthAppProxyClientProvider;

    private volatile Snapshot snapshot = new Snapshot(Map.of(), null, null);
    private volatile boolean clientResolved;
    private volatile OAuthAppProxyClient oAuthAppProxyClient;

    @Autowired
    public RemoteOAuth2AppsClient(ObjectProvider<OAuthAppProxyClient> oAuthAppProxyClientProvider) {
        this(oAuthAppProxyClientProvider, Clock.systemUTC());
    }

    RemoteOAuth2AppsClient(ObjectProvider<OAuthAppProxyClient> oAuthAppProxyClientProvider, Clock clock) {
        this.oAuthAppProxyClientProvider = oAuthAppProxyClientProvider;
        this.clock = clock;
    }

    public Map<String, OAuthApp> getOAuth2Apps() {
        OAuthAppProxyClient oAuthAppProxyClient = resolveClient();

        if (oAuthAppProxyClient == null) {
            return Map.of();
        }

        Snapshot currentSnapshot = snapshot;

        if (isFresh(currentSnapshot)) {
            return currentSnapshot.apps();
        }

        if (currentSnapshot.fetchedAt() != null || currentSnapshot.failedAt() != null) {
            if (!lock.tryLock()) {
                return currentSnapshot.apps();
            }
        } else {
            lock.lock();
        }

        try {
            currentSnapshot = snapshot;

            if (isFresh(currentSnapshot)) {
                return currentSnapshot.apps();
            }

            return refresh(oAuthAppProxyClient, currentSnapshot);
        } finally {
            lock.unlock();
        }
    }

    private OAuthAppProxyClient resolveClient() {
        if (!clientResolved) {
            try {
                oAuthAppProxyClient = oAuthAppProxyClientProvider.getIfAvailable();
            } catch (RuntimeException exception) {
                Class<?> exceptionClass = exception.getClass();

                warn("the OAuth apps proxy client could not be created: " + exceptionClass.getSimpleName(),
                    clock.instant());

                return null;
            }

            clientResolved = true;
        }

        return oAuthAppProxyClient;
    }

    private boolean isFresh(Snapshot currentSnapshot) {
        Instant now = clock.instant();

        if (currentSnapshot.fetchedAt() != null) {
            Duration sinceFetch = Duration.between(currentSnapshot.fetchedAt(), now);

            if (sinceFetch.compareTo(REFRESH_INTERVAL) < 0) {
                return true;
            }
        }

        if (currentSnapshot.failedAt() != null) {
            Duration sinceFailure = Duration.between(currentSnapshot.failedAt(), now);

            return sinceFailure.compareTo(RETRY_INTERVAL) < 0;
        }

        return false;
    }

    private Map<String, OAuthApp> refresh(OAuthAppProxyClient oAuthAppProxyClient, Snapshot currentSnapshot) {
        Instant now = clock.instant();

        Map<String, OAuthApp> apps;

        try {
            Map<String, OAuthApp> oAuthApps = oAuthAppProxyClient.getOAuthApps();

            apps = validApps(oAuthApps, now);
        } catch (RuntimeException exception) {
            snapshot = new Snapshot(currentSnapshot.apps(), currentSnapshot.fetchedAt(), now);

            warn(describe(exception), now);

            return currentSnapshot.apps();
        }

        snapshot = new Snapshot(apps, now, null);

        if (currentSnapshot.failedAt() != null) {
            lastWarnAtByReason.clear();

            log.info("Loaded {} predefined OAuth apps from the GitHub proxy again", apps.size());
        }

        return apps;
    }

    private Map<String, OAuthApp> validApps(Map<String, OAuthApp> oAuthApps, Instant now) {
        Map<String, OAuthApp> apps = new HashMap<>();
        List<String> ignoredNames = new ArrayList<>();

        for (Map.Entry<String, OAuthApp> entry : oAuthApps.entrySet()) {
            OAuthApp oAuthApp = entry.getValue();

            if (StringUtils.isBlank(entry.getKey()) || oAuthApp == null ||
                StringUtils.isBlank(oAuthApp.clientId()) || StringUtils.isBlank(oAuthApp.clientSecret())) {

                ignoredNames.add(String.valueOf(entry.getKey()));

                continue;
            }

            apps.put(entry.getKey(), oAuthApp);
        }

        if (!ignoredNames.isEmpty()) {
            ignoredNames.sort(null);

            warn("ignored entries without a clientId or clientSecret: " + String.join(", ", ignoredNames), now);
        }

        return Map.copyOf(apps);
    }

    private static String describe(RuntimeException exception) {
        if (exception instanceof RestClientResponseException restClientResponseException) {
            HttpStatusCode statusCode = restClientResponseException.getStatusCode();

            int status = statusCode.value();

            if (status == 401 || status == 403) {
                return "the proxy rejected the API key (HTTP " + status +
                    "), check github-proxy.oauth-apps.api-key";
            }

            if (statusCode.is3xxRedirection()) {
                return "the proxy answered with a redirect (HTTP " + status + "), check github-proxy.base-url";
            }

            if (statusCode.is2xxSuccessful()) {
                return "the proxy answered without a body (HTTP " + status + ")";
            }

            return "the proxy answered HTTP " + status;
        }

        Class<?> exceptionClass = exception.getClass();

        return "the request to the proxy failed: " + exceptionClass.getSimpleName();
    }

    private void warn(String reason, Instant now) {
        Instant previousWarnAt = lastWarnAtByReason.get(reason);

        if (previousWarnAt != null) {
            Duration sinceWarn = Duration.between(previousWarnAt, now);

            if (sinceWarn.compareTo(WARN_INTERVAL) < 0) {
                return;
            }
        }

        lastWarnAtByReason.put(reason, now);

        log.warn("Predefined OAuth apps from the GitHub proxy: {}", reason);
    }

    private record Snapshot(Map<String, OAuthApp> apps, Instant fetchedAt, Instant failedAt) {
    }
}
