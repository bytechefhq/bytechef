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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects and masks URLs according to an allowlist/scheme policy.
 *
 * @author Ivica Cardic
 */
public final class UrlDetector {

    private static final Logger log = LoggerFactory.getLogger(UrlDetector.class);

    /**
     * Time-bounded cache of malformed CIDR warnings. A misconfigured allowlist should produce one WARN per entry per
     * window so operators see the problem without being spammed per URL scanned; after {@link #CIDR_WARN_TTL_MILLIS}
     * the entry is eligible to re-emit so operators who have restarted/deployed after fixing the config continue to get
     * a warning if the same bad CIDR reappears in configuration. A pure per-JVM {@code Set} would suppress forever,
     * which is a real regression hazard: a persistent-broken configuration is easy to forget.
     */
    private static final long CIDR_WARN_TTL_MILLIS = 60L * 60L * 1000L;

    /**
     * Soft cap on the number of distinct malformed-CIDR strings we remember for TTL-based dedup. If an attacker can
     * influence allowlist contents they could otherwise grow this map with distinct bad strings indefinitely.
     */
    private static final int WARN_CACHE_MAX_SIZE = 4096;
    private static final Map<String, Long> WARNED_MALFORMED_CIDRS = new ConcurrentHashMap<>();

    private static final Pattern SCHEME_URL_PATTERN = Pattern.compile(
        "\\b[a-zA-Z][a-zA-Z0-9+.-]*://[^\\s<>\"']+",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern BARE_DOMAIN_PATTERN = Pattern.compile(
        "\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}(?:/[^\\s<>\"']*)?",
        Pattern.CASE_INSENSITIVE);

    /**
     * Common Latin prose abbreviations that {@link #BARE_DOMAIN_PATTERN} matches as bare domains. Tightening the TLD
     * class to a real allowlist (only ICANN-recognised TLDs) would be the principled fix but requires a maintained
     * 1500-entry list and a release train when ICANN delegates new TLDs; the abbreviation skip is a low-maintenance
     * defense against the high-traffic false positives. Keep entries lower-cased and without the trailing dot — the
     * detector strips the trailing dot before this check runs.
     */
    private static final Set<String> COMMON_PROSE_ABBREVIATIONS = Set.of(
        "e.g", "i.e", "a.m", "p.m", "et.al", "ph.d", "n.b");

    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b");

    private static final Pattern CIDR_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+");

    /**
     * Non-authority schemes (no {@code //}) commonly used as client-side injection vectors. Matched verbatim so the
     * caller can treat them as scheme-bearing URLs with no host; allowlisting must be done via {@code allowedSchemes}.
     */
    private static final Pattern SINGLE_COLON_SCHEME_PATTERN = Pattern.compile(
        "\\b(?:data|javascript|vbscript|mailto):[^\\s<>\"']+",
        Pattern.CASE_INSENSITIVE);

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

    private UrlDetector() {
    }

    public static List<UrlMatch> detectViolations(String content, UrlPolicy policy) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        List<UrlMatch> violations = new ArrayList<>();
        List<int[]> acceptedRanges = new ArrayList<>();

        collectSchemeUrlMatches(content, policy, violations, acceptedRanges);
        collectSingleColonSchemeMatches(content, policy, violations, acceptedRanges);
        collectIpv4Matches(content, policy, violations, acceptedRanges);
        collectBareDomainMatches(content, policy, violations, acceptedRanges);

        violations.sort(Comparator.comparingInt(UrlMatch::start));

        return violations;
    }

    public static String mask(String content, List<UrlMatch> matches) {
        if (content == null || content.isEmpty() || matches == null || matches.isEmpty()) {
            return content;
        }

        List<UrlMatch> sorted = new ArrayList<>(matches);

        sorted.sort(Comparator.comparingInt(UrlMatch::start)
            .reversed());

        StringBuilder builder = new StringBuilder(content);

        for (UrlMatch match : sorted) {
            builder.replace(match.start(), match.end(), "<URL>");
        }

        return builder.toString();
    }

    private static void collectSchemeUrlMatches(
        String content, UrlPolicy policy, List<UrlMatch> violations, List<int[]> acceptedRanges) {

        Matcher matcher = SCHEME_URL_PATTERN.matcher(content);

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
            String host = uri.getHost() == null ? "" : uri.getHost()
                .toLowerCase(Locale.ROOT);
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();

            if (policy.blockUserinfo() && uri.getUserInfo() != null) {
                violations.add(new UrlMatch(rawUrl, start, end, "USERINFO_BLOCKED"));

                continue;
            }

            if (!policy.allowedSchemes()
                .contains(scheme)) {
                violations.add(new UrlMatch(rawUrl, start, end, "SCHEME_NOT_ALLOWED"));

                continue;
            }

            if (!urlAllowed(host, path, policy)) {
                violations.add(new UrlMatch(rawUrl, start, end, "HOST_NOT_ALLOWED"));
            }
        }
    }

    private static void collectSingleColonSchemeMatches(
        String content, UrlPolicy policy, List<UrlMatch> violations, List<int[]> acceptedRanges) {

        Matcher matcher = SINGLE_COLON_SCHEME_PATTERN.matcher(content);

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

            if (policy.allowedSchemes()
                .contains(scheme)) {
                continue;
            }

            violations.add(new UrlMatch(rawUrl, start, end, "SCHEME_NOT_ALLOWED"));
        }
    }

    private static void collectIpv4Matches(
        String content, UrlPolicy policy, List<UrlMatch> violations, List<int[]> acceptedRanges) {

        Matcher matcher = IPV4_PATTERN.matcher(content);

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

            // Bare IPs have no scheme. Allow only if explicitly allowlisted (CIDR or exact host).
            if (urlAllowed(rawUrl, "", policy)) {
                continue;
            }

            addBareHostViolation(violations, rawUrl, start, end);
        }
    }

    /**
     * Bare IPs and bare domains have no scheme; the {@code UrlPolicy} compact constructor drops blank scheme entries,
     * so {@code allowedSchemes} can never contain {@code ""} — these hosts are always rejected as
     * {@code SCHEME_NOT_ALLOWED}. Centralised so the rationale lives in one place rather than being duplicated at every
     * callsite.
     */
    private static void addBareHostViolation(List<UrlMatch> violations, String rawUrl, int start, int end) {
        violations.add(new UrlMatch(rawUrl, start, end, "SCHEME_NOT_ALLOWED"));
    }

    private static void collectBareDomainMatches(
        String content, UrlPolicy policy, List<UrlMatch> violations, List<int[]> acceptedRanges) {

        Matcher matcher = BARE_DOMAIN_PATTERN.matcher(content);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (isContained(start, end, acceptedRanges)) {
                continue;
            }

            String rawUrl = matcher.group();

            // Strip a single trailing dot ("example.com." in a sentence).
            if (rawUrl.endsWith(".")) {
                rawUrl = rawUrl.substring(0, rawUrl.length() - 1);
                end = end - 1;
            }

            // Skip well-known Latin prose abbreviations that look like bare domains ("e.g.", "i.e.", "a.m.", etc.).
            // Without this skip every sentence containing one of these forms would emit a SCHEME_NOT_ALLOWED
            // violation — high-traffic noise that hides real bare-domain matches.
            if (COMMON_PROSE_ABBREVIATIONS.contains(rawUrl.toLowerCase(Locale.ROOT))) {
                continue;
            }

            acceptedRanges.add(new int[] {
                start, end
            });

            int slashIndex = rawUrl.indexOf('/');
            String host;
            String path;

            if (slashIndex < 0) {
                host = rawUrl.toLowerCase(Locale.ROOT);
                path = "";
            } else {
                host = rawUrl.substring(0, slashIndex)
                    .toLowerCase(Locale.ROOT);
                path = rawUrl.substring(slashIndex);
            }

            if (urlAllowed(host, path, policy)) {
                continue;
            }

            addBareHostViolation(violations, rawUrl, start, end);
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

    private static boolean urlAllowed(String host, String path, UrlPolicy policy) {
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

            if (CIDR_PATTERN.matcher(normalized)
                .matches()) {

                if (ipv4InCidr(host, normalized)) {
                    return true;
                }

                continue;
            }

            if (normalized.contains("/")) {
                int slash = normalized.indexOf('/');
                String entryHost = stripWwwPrefix(normalized.substring(0, slash));
                String entryPath = normalized.substring(slash);
                String entryPathWithSlash = entryPath.endsWith("/") ? entryPath : entryPath + "/";

                if (normalizedHost.equals(entryHost) && (path + "/").startsWith(entryPathWithSlash)) {
                    return true;
                }

                continue;
            }

            String normalizedEntry = stripWwwPrefix(normalized);

            if (normalizedHost.equals(normalizedEntry)) {
                return true;
            }

            if (policy.allowSubdomain() && normalizedHost.endsWith("." + normalizedEntry)) {
                return true;
            }
        }

        return false;
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

        try {
            int prefix = Integer.parseInt(cidr.substring(slash + 1));

            if (prefix < 0 || prefix > 32) {
                warnMalformedCidrOnce(cidr, "prefix out of range 0..32: " + prefix);

                return false;
            }

            long ipLong = ipv4ToLong(ip);
            long netLong = ipv4ToLong(cidr.substring(0, slash));
            long mask = prefix == 0 ? 0 : (-1L << (32 - prefix)) & 0xFFFFFFFFL;

            return (ipLong & mask) == (netLong & mask);
        } catch (NumberFormatException e) {
            warnMalformedCidrOnce(cidr, e.getClass()
                .getSimpleName() + ": " + e.getMessage());

            return false;
        }
    }

    private static void warnMalformedCidrOnce(String cidr, String reason) {
        long now = System.currentTimeMillis();
        Long previousAt = WARNED_MALFORMED_CIDRS.get(cidr);

        if (previousAt != null && now - previousAt < CIDR_WARN_TTL_MILLIS) {
            return;
        }

        // Defense-in-depth: if the cache exceeds its soft cap, drop all entries that are already outside the TTL. This
        // keeps bookkeeping bounded even when a misconfigured pipeline spams the detector with many distinct malformed
        // CIDR strings.
        if (WARNED_MALFORMED_CIDRS.size() >= WARN_CACHE_MAX_SIZE) {
            WARNED_MALFORMED_CIDRS.entrySet()
                .removeIf(entry -> now - entry.getValue() >= CIDR_WARN_TTL_MILLIS);

            // If every entry is still TTL-fresh (a burst of distinct malformed CIDRs landed inside the same warning
            // window), the TTL sweep above clears nothing and a naive putIfAbsent below would grow the map without
            // bound. Drop the oldest entry by timestamp. Under heavy concurrency the size() read and the subsequent
            // removeIf / min() chain are not atomic — multiple threads can each observe `size >= CAP` and each
            // evict, so the cap is best-effort rather than strict. This sacrifices both the exact-cap invariant and
            // the 1-WARN-per-TTL guarantee for the displaced entry; both are acceptable because the alternative is
            // unbounded memory under attack-style input.
            if (WARNED_MALFORMED_CIDRS.size() >= WARN_CACHE_MAX_SIZE) {
                WARNED_MALFORMED_CIDRS.entrySet()
                    .stream()
                    .min(Map.Entry.comparingByValue())
                    .ifPresent(oldest -> WARNED_MALFORMED_CIDRS.remove(oldest.getKey(), oldest.getValue()));
            }
        }

        // putIfAbsent isn't enough because we want to refresh the timestamp on each emission; a compareAndSet on the
        // stored Long keeps the 1-WARN-per-TTL guarantee even under concurrent scans of the same misconfigured entry.
        if (previousAt == null
            ? WARNED_MALFORMED_CIDRS.putIfAbsent(cidr, now) == null
            : WARNED_MALFORMED_CIDRS.replace(cidr, previousAt, now)) {

            log.warn(
                "UrlDetector allowlist entry '{}' is not a valid IPv4 CIDR ({}); no URL will match this entry. "
                    + "Fix the configuration or remove the entry.",
                cidr, reason);
        }
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
}
