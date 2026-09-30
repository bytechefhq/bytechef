/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.observability.aws;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/**
 * Computes the AWS Signature Version 4 headers of a single HTTP request.
 *
 * <p>
 * The signature covers the method, path, query, the given headers, the {@code host} header and the SHA-256 hash of the
 * exact payload bytes, so the payload must be signed after it has been serialized and compressed, and must not change
 * afterwards.
 *
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_sigv-create-signed-request.html">Create a
 *      signed AWS API request</a>
 *
 * @version ee
 *
 * @author Igor Beslic
 */
public final class AwsSigV4Signer {

    static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneOffset.UTC);
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Headers proxies and load balancers may add or rewrite; like the AWS SDK signers, they are sent but not signed.
     */
    private static final Set<String> UNSIGNED_HEADER_NAMES = Set.of(
        "connection", "expect", "user-agent", "x-amzn-trace-id");

    private final Clock clock;
    private final String region;
    private final String service;

    public AwsSigV4Signer(String region, String service) {
        this(region, service, Clock.systemUTC());
    }

    AwsSigV4Signer(String region, String service, Clock clock) {
        this.clock = clock;
        this.region = region;
        this.service = service;
    }

    /**
     * Signs the request and returns the headers to add to it: {@code Authorization}, {@code X-Amz-Content-Sha256},
     * {@code X-Amz-Date} and, for temporary credentials, {@code X-Amz-Security-Token}.
     *
     * @param method  the HTTP method
     * @param uri     the request URI, the {@code host} header is derived from it the same way the JDK HTTP client does
     * @param headers the headers to sign, they must be sent with the request unchanged
     * @param payload the exact request body bytes
     */
    public Map<String, String> sign(
        String method, URI uri, Map<String, String> headers, byte[] payload, Credentials credentials) {

        Instant now = clock.instant();

        String amzDate = AMZ_DATE_FORMATTER.format(now);
        String payloadHash = HEX_FORMAT.formatHex(sha256(payload));

        Map<String, String> signatureHeaders = new LinkedHashMap<>();

        signatureHeaders.put("X-Amz-Content-Sha256", payloadHash);
        signatureHeaders.put("X-Amz-Date", amzDate);

        if (credentials.sessionToken() != null) {
            signatureHeaders.put("X-Amz-Security-Token", credentials.sessionToken());
        }

        SortedMap<String, String> canonicalHeaders = new TreeMap<>();

        canonicalHeaders.put("host", getHostHeader(uri));

        headers.forEach((name, value) -> putCanonicalHeader(canonicalHeaders, name, value));
        signatureHeaders.forEach((name, value) -> putCanonicalHeader(canonicalHeaders, name, value));

        String signedHeaders = String.join(";", canonicalHeaders.keySet());

        String canonicalRequest = String.join(
            "\n", method, getCanonicalUri(uri), getCanonicalQuery(uri), getCanonicalHeaders(canonicalHeaders),
            signedHeaders, payloadHash);

        String dateStamp = DATE_STAMP_FORMATTER.format(now);

        String credentialScope = String.join("/", dateStamp, region, service, "aws4_request");

        String stringToSign = String.join(
            "\n", ALGORITHM, amzDate, credentialScope,
            HEX_FORMAT.formatHex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8))));

        byte[] signingKey = getSigningKey(credentials.secretAccessKey(), dateStamp);

        String signature = HEX_FORMAT.formatHex(hmacSha256(signingKey, stringToSign));

        signatureHeaders.put(
            "Authorization",
            ALGORITHM + " Credential=" + credentials.accessKeyId() + "/" + credentialScope + ", SignedHeaders=" +
                signedHeaders + ", Signature=" + signature);

        return signatureHeaders;
    }

    /**
     * Mirrors the JDK HTTP/1.1 client, which omits the port from the {@code Host} header when it is the scheme default.
     */
    static String getHostHeader(URI uri) {
        int port = uri.getPort();

        boolean defaultPort = port == -1 || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443) ||
            ("http".equalsIgnoreCase(uri.getScheme()) && port == 80);

        return defaultPort ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * All services except S3 expect each path segment to be URI-encoded twice; the raw path is already encoded once.
     */
    private static String getCanonicalUri(URI uri) {
        String rawPath = uri.getRawPath();

        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }

        List<String> encodedSegments = new ArrayList<>();

        for (String segment : rawPath.split("/", -1)) {
            encodedSegments.add(uriEncode(segment));
        }

        return String.join("/", encodedSegments);
    }

    private static String getCanonicalQuery(URI uri) {
        String rawQuery = uri.getRawQuery();

        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }

        SortedMap<String, List<String>> parameters = new TreeMap<>();

        for (String parameter : rawQuery.split("&")) {
            int separatorIndex = parameter.indexOf('=');

            String name = separatorIndex < 0 ? parameter : parameter.substring(0, separatorIndex);
            String value = separatorIndex < 0 ? "" : parameter.substring(separatorIndex + 1);

            parameters.computeIfAbsent(uriEncode(uriDecode(name)), key -> new ArrayList<>())
                .add(uriEncode(uriDecode(value)));
        }

        return parameters.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue()
                .stream()
                .sorted()
                .map(value -> entry.getKey() + "=" + value))
            .collect(Collectors.joining("&"));
    }

    private static String getCanonicalHeaders(SortedMap<String, String> canonicalHeaders) {
        StringBuilder stringBuilder = new StringBuilder();

        canonicalHeaders.forEach((name, value) -> stringBuilder.append(name)
            .append(':')
            .append(value)
            .append('\n'));

        return stringBuilder.toString();
    }

    private byte[] getSigningKey(String secretAccessKey, String dateStamp) {
        byte[] dateKey = hmacSha256(("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] dateRegionKey = hmacSha256(dateKey, region);
        byte[] dateRegionServiceKey = hmacSha256(dateRegionKey, service);

        return hmacSha256(dateRegionServiceKey, "aws4_request");
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(key, HMAC_SHA256));

            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    private static void putCanonicalHeader(SortedMap<String, String> canonicalHeaders, String name, String value) {
        String lowerCaseName = name.toLowerCase(Locale.ROOT);

        if (UNSIGNED_HEADER_NAMES.contains(lowerCaseName)) {
            return;
        }

        canonicalHeaders.put(
            lowerCaseName, value.trim()
                .replaceAll("\\s+", " "));
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return messageDigest.digest(data);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String uriDecode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    /**
     * RFC 3986 encoding: only the unreserved characters are left as they are, everything else is percent-encoded with
     * upper case hex digits.
     */
    private static String uriEncode(String value) {
        StringBuilder stringBuilder = new StringBuilder();

        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            char character = (char) (valueByte & 0xFF);

            if ((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z') ||
                (character >= '0' && character <= '9') || character == '-' || character == '_' || character == '.' ||
                character == '~') {

                stringBuilder.append(character);
            } else {
                stringBuilder.append('%')
                    .append(HexFormat.of()
                        .withUpperCase()
                        .toHexDigits(valueByte));
            }
        }

        return stringBuilder.toString();
    }

    /**
     * AWS credentials used for signing; {@code sessionToken} is set only for temporary (STS) credentials.
     */
    public record Credentials(String accessKeyId, String secretAccessKey, @Nullable String sessionToken) {

        @Override
        public String toString() {
            return "Credentials[accessKeyId=" + accessKeyId + ", secretAccessKey=****, sessionToken=" +
                (sessionToken == null ? "null" : "****") + "]";
        }
    }
}
