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

package com.bytechef.component.ai.agent.guardrails.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects and masks URLs according to an allowlist/scheme policy.
 *
 * @author Ivica Cardic
 */
public final class UrlDetectorUtils {

    private static final Logger log = LoggerFactory.getLogger(UrlDetectorUtils.class);

    private static final Cache<String, Boolean> WARNED_MALFORMED_CIDRS = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(4096)
        .build();
    private static final Cache<String, Boolean> WARNED_MALFORMED_HOSTS = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(4096)
        .build();
    private static final Cache<String, Boolean> WARNED_MALFORMED_ALLOWLIST_ENTRIES = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(4096)
        .build();
    private static final Pattern SCHEME_URL_PATTERN = Pattern.compile(
        "\\b[a-zA-Z][a-zA-Z0-9+.-]*://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CIDR_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+");
    private static final Pattern SINGLE_COLON_SCHEME_PATTERN = Pattern.compile(
        "\\b(?:data|javascript|vbscript|mailto):[^\\s<>\"']+",
        Pattern.CASE_INSENSITIVE);

    private UrlDetectorUtils() {
    }

    public static List<UrlMatch> detectViolations(String content, UrlPolicy policy) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        CharSequence bounded = RegexParserUtils.bounded(content);

        List<UrlMatch> violations = new ArrayList<>();
        List<int[]> acceptedRanges = new ArrayList<>();

        collectSchemeUrlMatches(bounded, policy, violations, acceptedRanges);
        collectSingleColonSchemeMatches(bounded, policy, violations, acceptedRanges);

        violations.sort(Comparator.comparingInt(UrlMatch::start));

        return violations;
    }

    public static String mask(String content, List<UrlMatch> matches) {
        if (content == null || content.isEmpty() || matches == null || matches.isEmpty()) {
            return content;
        }

        List<UrlMatch> sorted = new ArrayList<>(matches);

        sorted.sort(
            Comparator.comparingInt(UrlMatch::start)
                .reversed());

        StringBuilder builder = new StringBuilder(content);

        for (UrlMatch match : sorted) {
            builder.replace(match.start(), match.end(), "<URL>");
        }

        return builder.toString();
    }

    private static void collectSchemeUrlMatches(
        CharSequence bounded, UrlPolicy policy, List<UrlMatch> violations, List<int[]> acceptedRanges) {

        Matcher matcher = SCHEME_URL_PATTERN.matcher(bounded);

        while (matcher.find()) {
            String rawUrl = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            acceptedRanges.add(new int[] {
                start, end
            });

            URI uri;

            try {
                uri = new URI(rawUrl);
            } catch (URISyntaxException e) {
                violations.add(new UrlMatch(rawUrl, start, end, "MALFORMED_URL"));

                continue;
            }

            String scheme = uri.getScheme() == null ? "" : uri.getScheme()
                .toLowerCase(Locale.ROOT);
            String host = normalizeHost(uri.getHost());
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();

            if (policy.blockUserinfo() && uri.getUserInfo() != null) {
                violations.add(new UrlMatch(rawUrl, start, end, "USERINFO_BLOCKED"));

                continue;
            }

            List<String> allowedSchemes = policy.allowedSchemes();

            if (!allowedSchemes.contains(scheme)) {
                violations.add(new UrlMatch(rawUrl, start, end, "SCHEME_NOT_ALLOWED"));

                continue;
            }

            if (!urlAllowed(host, uri.getPort(), path, policy)) {
                violations.add(new UrlMatch(rawUrl, start, end, "HOST_NOT_ALLOWED"));
            }
        }
    }

    private static void collectSingleColonSchemeMatches(
        CharSequence bounded, UrlPolicy policy, List<UrlMatch> violations, List<int[]> acceptedRanges) {

        Matcher matcher = SINGLE_COLON_SCHEME_PATTERN.matcher(bounded);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (isContained(start, end, acceptedRanges)) {
                continue;
            }

            String rawUrl = matcher.group();

            acceptedRanges.add(new int[] {
                start, end
            });

            int colon = rawUrl.indexOf(':');
            String scheme = rawUrl.substring(0, colon)
                .toLowerCase(Locale.ROOT);

            List<String> allowedSchemes = policy.allowedSchemes();

            if (allowedSchemes.contains(scheme)) {
                continue;
            }

            violations.add(new UrlMatch(rawUrl, start, end, "SCHEME_NOT_ALLOWED"));
        }
    }

    private static boolean isContained(int start, int end, List<int[]> ranges) {
        for (int[] range : ranges) {
            if (start >= range[0] && end <= range[1]) {
                return true;
            }
        }

        return false;
    }

    private static boolean urlAllowed(String host, int port, String path, UrlPolicy policy) {
        List<String> allowed = policy.allowedUrls();

        if (allowed == null || allowed.isEmpty()) {
            return false;
        }

        String normalizedHost = stripWwwPrefix(host);

        for (String entry : allowed) {
            String normalized = entry == null ? "" : entry.toLowerCase(Locale.ROOT)
                .trim();

            if (normalized.isEmpty()) {
                continue;
            }

            Matcher matcher = CIDR_PATTERN.matcher(normalized);

            if (matcher.matches()) {
                if (ipv4InCidr(host, normalized)) {
                    return true;
                }

                continue;
            }

            if (normalized.contains("://")) {
                if (matchesSchemeEntry(normalizedHost, port, path, normalized, policy.allowSubdomain())) {
                    return true;
                }

                continue;
            }

            if (normalized.contains("/")) {
                int slash = normalized.indexOf('/');
                String entryHost = stripWwwPrefix(normalizeHost(normalized.substring(0, slash)));
                String entryPath = normalized.substring(slash);
                String entryPathWithSlash = entryPath.endsWith("/") ? entryPath : entryPath + "/";

                if (normalizedHost.equals(entryHost) && (path + "/").startsWith(entryPathWithSlash)) {
                    return true;
                }

                continue;
            }

            String normalizedEntry = stripWwwPrefix(normalizeHost(normalized));

            if (normalizedHost.equals(normalizedEntry)) {
                return true;
            }

            if (policy.allowSubdomain() && normalizedHost.endsWith("." + normalizedEntry)) {
                return true;
            }
        }

        return false;
    }

    private static final String INVALID_HOST_SENTINEL = " __invalid_host__ ";

    private static String normalizeHost(String host) {
        if (host == null || host.isEmpty()) {
            return "";
        }

        String nfc = Normalizer.normalize(host, Normalizer.Form.NFC);

        try {
            return IDN.toASCII(nfc, IDN.ALLOW_UNASSIGNED)
                .toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            log.warn(
                "Host '{}' failed IDN.toASCII normalization ({}); treating as invalid (will not match any allowlist " +
                    "entry). A degraded ASCII comparison would weaken Cyrillic-homograph defense.",
                host, exception.getMessage());

            return INVALID_HOST_SENTINEL;
        }
    }

    private static boolean matchesSchemeEntry(
        String normalizedHost, int inputPort, String inputPath, String entry, boolean allowSubdomain) {

        URI entryUri;

        try {
            entryUri = new URI(entry);
        } catch (URISyntaxException exception) {
            warnMalformedAllowlistEntryOnce(entry, exception.getMessage());

            return false;
        }

        String entryHostRaw = entryUri.getHost();

        if (entryHostRaw == null || entryHostRaw.isEmpty()) {
            warnMalformedAllowlistEntryOnce(entry, "URI has no host component");

            return false;
        }

        String entryHost = stripWwwPrefix(normalizeHost(entryHostRaw));

        boolean hostMatches = normalizedHost.equals(entryHost) ||
            (allowSubdomain && normalizedHost.endsWith("." + entryHost));

        if (!hostMatches) {
            return false;
        }

        int entryPort = entryUri.getPort();

        if (entryPort != -1 && entryPort != inputPort) {
            return false;
        }

        String entryPath = entryUri.getRawPath() == null ? "" : entryUri.getRawPath();

        if (entryPath.isEmpty() || entryPath.equals("/")) {
            return true;
        }

        String entryPathWithSlash = entryPath.endsWith("/") ? entryPath : entryPath + "/";

        return (inputPath + "/").startsWith(entryPathWithSlash);
    }

    private static String stripWwwPrefix(String host) {
        return host != null && host.startsWith("www.") ? host.substring(4) : host == null ? "" : host;
    }

    private static boolean ipv4InCidr(String ip, String cidr) {
        int slash = cidr.indexOf('/');

        if (slash < 0) {
            warnMalformedCidrOnce(cidr, "missing '/' separator");

            return false;
        }

        int prefix;

        try {
            prefix = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException exception) {
            warnMalformedCidrOnce(cidr, "unparseable prefix: " + exception.getMessage());

            return false;
        }

        if (prefix < 0 || prefix > 32) {
            warnMalformedCidrOnce(cidr, "prefix out of range 0..32: " + prefix);

            return false;
        }

        long netLong;

        try {
            netLong = ipv4ToLong(cidr.substring(0, slash));
        } catch (NumberFormatException exception) {
            warnMalformedCidrOnce(cidr, "unparseable network address: " + exception.getMessage());

            return false;
        }

        long ipLong;

        try {
            ipLong = ipv4ToLong(ip);
        } catch (NumberFormatException exception) {
            warnMalformedHostOnce(ip, exception.getMessage());

            return false;
        }

        long mask = prefix == 0 ? 0 : (-1L << (32 - prefix)) & 0xFFFFFFFFL;

        return (ipLong & mask) == (netLong & mask);
    }

    private static void warnMalformedCidrOnce(String cidr, String reason) {
        Map<String, Boolean> warnedCidrs = WARNED_MALFORMED_CIDRS.asMap();

        if (warnedCidrs.putIfAbsent(cidr, Boolean.TRUE) != null) {
            return;
        }

        log.warn(
            "UrlDetector allowlist entry '{}' is not a valid IPv4 CIDR ({}); no URL will match this entry. "
                + "Fix the configuration or remove the entry.",
            cidr, reason);
    }

    private static void warnMalformedHostOnce(String host, String reason) {
        Map<String, Boolean> warnedHosts = WARNED_MALFORMED_HOSTS.asMap();

        if (warnedHosts.putIfAbsent(host, Boolean.TRUE) != null) {
            return;
        }

        log.warn(
            "UrlDetector encountered a URL whose host '{}' is not a parseable IPv4 address ({}); the host "
                + "cannot match any IPv4 CIDR allowlist entry. This is usually benign noise but a sustained stream "
                + "may indicate probing.",
            host, reason);
    }

    private static void warnMalformedAllowlistEntryOnce(String entry, String reason) {
        Map<String, Boolean> warnedEntries = WARNED_MALFORMED_ALLOWLIST_ENTRIES.asMap();

        if (warnedEntries.putIfAbsent(entry, Boolean.TRUE) != null) {
            return;
        }

        log.warn(
            "UrlDetector allowlist entry '{}' is not a valid absolute URI ({}); no URL will match this entry. "
                + "Fix the configuration or remove the entry.",
            entry, reason);
    }

    private static long ipv4ToLong(String ipv4) {
        String[] parts = ipv4.split("\\.");

        if (parts.length != 4) {
            throw new NumberFormatException("not IPv4: " + ipv4);
        }

        long out = 0;

        for (String part : parts) {
            int octet = Integer.parseInt(part);

            if (octet < 0 || octet > 255) {
                throw new NumberFormatException("octet out of range: " + part);
            }

            out = (out << 8) | octet;
        }

        return out;
    }

    public record UrlPolicy(
        List<String> allowedUrls, List<String> allowedSchemes,
        boolean blockUserinfo, boolean allowSubdomain) {

        public UrlPolicy {
            allowedUrls = allowedUrls != null ? List.copyOf(allowedUrls) : List.of();

            if (allowedSchemes == null) {
                allowedSchemes = List.of();
            } else {
                List<String> normalized = new ArrayList<>(allowedSchemes.size());

                for (String scheme : allowedSchemes) {
                    if (scheme != null && !scheme.isBlank()) {
                        normalized.add(scheme.trim()
                            .toLowerCase(Locale.ROOT));
                    }
                }

                allowedSchemes = List.copyOf(normalized);
            }
        }
    }

    public record UrlMatch(String url, int start, int end, String reason) {
    }
}
