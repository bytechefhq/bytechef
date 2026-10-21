/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Guards the app-facing proxy so that the credentials of a bound connection can only ever be sent to the connection's
 * own base URI.
 *
 * <p>
 * The component HTTP client returns any URL containing {@code ://} unchanged while still attaching the connection's
 * credentials, and joins every other URL to the connection's base URI by plain string concatenation. The proxy must
 * therefore only ever pass a path that {@link #validate(String)} accepted, and {@link #pin(String, String)} proves that
 * the joined URI still addresses the same scheme, host and port and stays under the base path.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ProxyPathValidator {

    private static final int MAX_ECHOED_PATH_LENGTH = 200;

    /**
     * ASCII only. {@code ?} and {@code #} are excluded because the query goes into {@code ProxyRequest.query}, and
     * {@code @}, backslash, whitespace and control characters are excluded because they change how a URL is parsed.
     */
    private static final String ALLOWED_PUNCTUATION = "._~!$&'()*+,;=:%/-";

    /**
     * Percent-encoded bytes that would, once a server decodes them, change the structure of the path: a new segment, a
     * query, a fragment, userinfo or a second layer of encoding.
     */
    private static final String FORBIDDEN_ENCODED_BYTES = "/\\?#@%";

    private ProxyPathValidator() {
    }

    /**
     * Returns the path unchanged when it is a plain absolute path, or throws
     * {@link AppErrorType#APP_PROXY_PATH_REJECTED}.
     */
    public static String validate(@Nullable String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("://")
            || !containsOnlyAllowedCharacters(path)) {

            throw rejected(path);
        }

        String[] segments = path.split("/", -1);

        for (int i = 1; i < segments.length; i++) {
            // An empty segment is only allowed as the trailing one ("/a/"), never in the middle ("/a//b").
            if (segments[i].isEmpty() && i != segments.length - 1) {
                throw rejected(path);
            }

            checkDecodedSegment(decodeSegment(segments[i], path), path);
        }

        return path;
    }

    /**
     * Joins {@code path} to {@code baseUri} and proves that the result shares scheme, host and port with the base and
     * stays under the base path, otherwise throws {@link AppErrorType#APP_PROXY_PATH_REJECTED}. A base URI with
     * userinfo, a query or a fragment is rejected, never silently stripped.
     */
    public static URI pin(@Nullable String baseUri, @Nullable String path) {
        URI base = parseBase(baseUri);

        String validatedPath = validate(path);
        String basePath = stripTrailingSlashes(base.getRawPath());

        URI resolved;

        try {
            resolved = new URI(base.getScheme() + "://" + base.getRawAuthority() + basePath + validatedPath);
        } catch (URISyntaxException uriSyntaxException) {
            throw rejected(path);
        }

        String resolvedPath = resolved.getRawPath();

        if (!base.getScheme()
            .equals(resolved.getScheme())
            || !base.getHost()
                .equalsIgnoreCase(resolved.getHost())
            || base.getPort() != resolved.getPort() || resolved.getUserInfo() != null || resolved.getQuery() != null
            || resolved.getFragment() != null || resolvedPath == null
            || !resolvedPath.equals(resolved.normalize()
                .getRawPath())
            || !(resolvedPath.equals(basePath) || resolvedPath.startsWith(basePath + "/"))) {

            throw rejected(path);
        }

        return resolved;
    }

    /**
     * Upstreams drop path parameters ({@code ;...}), trim whitespace and apply Unicode compatibility folding before
     * they resolve dot segments, so the segment is checked in every form an upstream might reduce it to.
     */
    private static void checkDecodedSegment(String decodedSegment, String path) {
        String normalizedSegment = Normalizer.normalize(decodedSegment, Normalizer.Form.NFKC);

        if (containsAny(normalizedSegment, "/\\@")) {
            throw rejected(path);
        }

        String[] reducedSegments = {
            Normalizer.normalize(cutAtSemicolon(decodedSegment), Normalizer.Form.NFKC)
                .strip(),
            cutAtSemicolon(normalizedSegment).strip()
        };

        for (String reducedSegment : reducedSegments) {
            if (reducedSegment.equals(".") || reducedSegment.equals("..") || containsAny(reducedSegment, "/\\@;")) {
                throw rejected(path);
            }
        }
    }

    private static String cutAtSemicolon(String segment) {
        int index = segment.indexOf(';');

        return index < 0 ? segment : segment.substring(0, index);
    }

    private static boolean containsAny(String value, String characters) {
        for (int i = 0; i < characters.length(); i++) {
            if (value.indexOf(characters.charAt(i)) >= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Beyond ASCII only letters, digits and combining marks are allowed. Every non-ASCII punctuation or symbol
     * character is a potential look-alike for {@code .}, {@code /}, {@code \}, {@code @} or {@code ;} that NFKC does
     * not fold (for example U+3002), so none of them can be told apart from the real thing on a lenient upstream.
     */
    private static boolean isPermittedNonAscii(int codePoint) {
        if (codePoint < 0x80) {
            return true;
        }

        int type = Character.getType(codePoint);

        return Character.isLetterOrDigit(codePoint) || type == Character.NON_SPACING_MARK
            || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK;
    }

    private static boolean containsOnlyAllowedCharacters(String path) {
        for (int i = 0; i < path.length(); i++) {
            char character = path.charAt(i);

            boolean allowed = (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9') || ALLOWED_PUNCTUATION.indexOf(character) >= 0;

            if (!allowed) {
                return false;
            }
        }

        return true;
    }

    private static String decodeSegment(String segment, String path) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(segment.length());

        for (int i = 0; i < segment.length(); i++) {
            char character = segment.charAt(i);

            if (character != '%') {
                bytes.write(character);

                continue;
            }

            if (i + 2 >= segment.length()) {
                throw rejected(path);
            }

            int high = Character.digit(segment.charAt(i + 1), 16);
            int low = Character.digit(segment.charAt(i + 2), 16);

            if (high < 0 || low < 0) {
                throw rejected(path);
            }

            int value = (high << 4) | low;

            if (value < 0x20 || value == 0x7f || (value < 0x80 && FORBIDDEN_ENCODED_BYTES.indexOf(value) >= 0)) {
                throw rejected(path);
            }

            bytes.write(value);

            i += 2;
        }

        String decodedSegment;

        try {
            decodedSegment = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes.toByteArray()))
                .toString();
        } catch (CharacterCodingException characterCodingException) {
            throw rejected(path);
        }

        decodedSegment.codePoints()
            .forEach(codePoint -> {
                int type = Character.getType(codePoint);

                if (Character.isISOControl(codePoint) || type == Character.FORMAT
                    || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR
                    || type == Character.UNASSIGNED || type == Character.PRIVATE_USE
                    || !isPermittedNonAscii(codePoint)) {

                    throw rejected(path);
                }
            });

        return decodedSegment;
    }

    private static URI parseBase(@Nullable String baseUri) {
        if (baseUri == null || baseUri.isBlank()) {
            throw rejectedBase();
        }

        URI base;

        try {
            base = new URI(baseUri);
        } catch (URISyntaxException uriSyntaxException) {
            throw rejectedBase();
        }

        String scheme = base.getScheme() == null ? "" : base.getScheme()
            .toLowerCase(Locale.ROOT);

        if (!(scheme.equals("https") || scheme.equals("http")) || base.isOpaque()
            || base.getHost() == null || base.getHost()
                .isEmpty()
            || base.getUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null) {

            throw rejectedBase();
        }

        return base;
    }

    private static ConfigurationException rejected(@Nullable String path) {
        return new ConfigurationException("Proxy path rejected: " + echo(path), AppErrorType.APP_PROXY_PATH_REJECTED);
    }

    private static ConfigurationException rejectedBase() {
        return new ConfigurationException(
            "Proxy path rejected: the connection base URI cannot be used for proxying",
            AppErrorType.APP_PROXY_PATH_REJECTED);
    }

    private static String echo(@Nullable String path) {
        if (path == null) {
            return "null";
        }

        StringBuilder echoed = new StringBuilder();

        for (int i = 0; i < path.length() && i < MAX_ECHOED_PATH_LENGTH; i++) {
            char character = path.charAt(i);

            echoed.append(character < 0x20 || character >= 0x7f ? '?' : character);
        }

        if (path.length() > MAX_ECHOED_PATH_LENGTH) {
            echoed.append("...");
        }

        return echoed.toString();
    }

    private static String stripTrailingSlashes(@Nullable String rawPath) {
        if (rawPath == null) {
            return "";
        }

        int end = rawPath.length();

        while (end > 0 && rawPath.charAt(end - 1) == '/') {
            end--;
        }

        return rawPath.substring(0, end);
    }
}
