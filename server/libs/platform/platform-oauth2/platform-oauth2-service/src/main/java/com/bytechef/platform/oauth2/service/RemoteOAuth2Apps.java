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

import com.bytechef.config.ApplicationProperties.Oauth2.OAuth2App;
import com.bytechef.platform.githubproxy.client.OAuthAppProxyClient;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Predefined OAuth2 apps served by the GitHub proxy, cached in memory. The last good copy is kept when a refresh fails.
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteOAuth2Apps {

    private static final Logger log = LoggerFactory.getLogger(RemoteOAuth2Apps.class);

    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(60);
    private static final Duration WARN_INTERVAL = Duration.ofHours(1);

    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();
    private final ObjectProvider<OAuthAppProxyClient> oAuthAppProxyClientProvider;

    private volatile Snapshot snapshot = new Snapshot(Map.of(), null, null);
    private volatile Instant lastWarnAt;
    private volatile boolean clientResolved;
    private volatile OAuthAppProxyClient oAuthAppProxyClient;

    @Autowired
    public RemoteOAuth2Apps(ObjectProvider<OAuthAppProxyClient> oAuthAppProxyClientProvider) {
        this(oAuthAppProxyClientProvider, Clock.systemUTC());
    }

    RemoteOAuth2Apps(ObjectProvider<OAuthAppProxyClient> oAuthAppProxyClientProvider, Clock clock) {
        this.oAuthAppProxyClientProvider = oAuthAppProxyClientProvider;
        this.clock = clock;
    }

    public Map<String, OAuth2App> getOAuth2Apps() {
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
                warn(exception, clock.instant());

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

    private Map<String, OAuth2App> refresh(OAuthAppProxyClient oAuthAppProxyClient, Snapshot currentSnapshot) {
        Instant now = clock.instant();

        try {
            Map<String, OAuthApp> oAuthApps = oAuthAppProxyClient.getOAuthApps();

            Map<String, OAuth2App> apps = convert(oAuthApps);

            snapshot = new Snapshot(apps, now, null);

            return apps;
        } catch (RuntimeException exception) {
            snapshot = new Snapshot(currentSnapshot.apps(), currentSnapshot.fetchedAt(), now);

            warn(exception, now);

            return currentSnapshot.apps();
        }
    }

    private static Map<String, OAuth2App> convert(Map<String, OAuthApp> oAuthApps) {
        if (oAuthApps == null) {
            return Map.of();
        }

        Map<String, OAuth2App> apps = new HashMap<>();

        for (Map.Entry<String, OAuthApp> entry : oAuthApps.entrySet()) {
            OAuthApp oAuthApp = entry.getValue();

            if (oAuthApp == null || StringUtils.isBlank(oAuthApp.clientId()) ||
                StringUtils.isBlank(oAuthApp.clientSecret())) {

                continue;
            }

            OAuth2App oAuth2App = new OAuth2App();

            oAuth2App.setClientId(oAuthApp.clientId());
            oAuth2App.setClientSecret(oAuthApp.clientSecret());

            apps.put(entry.getKey(), oAuth2App);
        }

        return Map.copyOf(apps);
    }

    private void warn(RuntimeException exception, Instant now) {
        Instant previousWarnAt = lastWarnAt;

        if (previousWarnAt != null) {
            Duration sinceWarn = Duration.between(previousWarnAt, now);

            if (sinceWarn.compareTo(WARN_INTERVAL) < 0) {
                return;
            }
        }

        lastWarnAt = now;

        Class<?> exceptionClass = exception.getClass();

        String reason = exceptionClass.getSimpleName();

        if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
            HttpStatusCode statusCode = httpStatusCodeException.getStatusCode();

            reason += " (status " + statusCode.value() + ")";
        }

        log.warn("Failed to load predefined OAuth apps from the GitHub proxy: {}", reason);
    }

    private record Snapshot(Map<String, OAuth2App> apps, Instant fetchedAt, Instant failedAt) {
    }
}
