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

package com.bytechef.platform.oauth2.util;

import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class OAuth2UrlValidator {

    static final String ALLOWED_HOSTS_HINT =
        ". If this is a trusted internal server, an administrator can add its host to the " +
            "bytechef.oauth2.discovery.allowed-hosts setting (BYTECHEF_OAUTH2_DISCOVERY_ALLOWED_HOSTS).";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final Set<String> DEV_HOSTS = Set.of("localhost", "127.0.0.1");

    private final Set<String> allowedHosts;

    public OAuth2UrlValidator(@Nullable Collection<String> configuredHosts, boolean devProfile) {
        Set<String> hosts = new HashSet<>();

        if (configuredHosts != null) {
            hosts.addAll(configuredHosts);
        }

        if (devProfile) {
            hosts.addAll(DEV_HOSTS);
        }

        this.allowedHosts = Set.copyOf(hosts);
    }

    public void validate(String url) {
        if (!UrlValidatorUtils.isValid(url, allowedHosts)) {
            throw new ConfigurationException(
                "This server address isn't allowed: " + url + ALLOWED_HOSTS_HINT, OAuth2ErrorType.URL_NOT_ALLOWED);
        }

        URI uri = URI.create(url);

        if (!"https".equalsIgnoreCase(uri.getScheme()) && !isAllowlisted(uri.getHost())) {
            throw new ConfigurationException(
                "This server address must use https: " + url + ALLOWED_HOSTS_HINT, OAuth2ErrorType.URL_NOT_ALLOWED);
        }
    }

    private boolean isAllowlisted(@Nullable String host) {
        if (host == null) {
            return false;
        }

        for (String allowedHost : allowedHosts) {
            if (allowedHost.equalsIgnoreCase(host)) {
                return true;
            }
        }

        return false;
    }
}
