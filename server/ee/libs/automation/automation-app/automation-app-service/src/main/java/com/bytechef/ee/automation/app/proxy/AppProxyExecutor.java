/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.exception.HttpResponseTooLargeException;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.util.RefreshCredentialsUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Sends an app's raw HTTP request through one of its bound connections, with the connection's credentials attached.
 *
 * <p>
 * The credentials must only ever reach the connection's own base URI. The request is sent to the absolute URI
 * {@link ProxyPathValidator#pin(String, String)} proved to stay on the base URI's scheme, host and port and under its
 * path: the validated string is exactly the string sent. Query parameters travel separately and are URL-encoded by the
 * component HTTP client, never concatenated into that string. Redirects are never followed, since a redirect could
 * carry the credentials to another host; a 3xx is handed back to the app like any other response.
 *
 * <p>
 * The client's own authorization is disabled and the proxy applies the connection's credentials itself: the client's
 * authorization path also installs an error interceptor that turns every 4xx and 5xx into an exception before the body
 * is read, which would hide the upstream error from the app. Applied credentials win over any app header or query
 * parameter of the same name, compared case-insensitively. Because the platform's token refresh lives in an aspect the
 * proxy does not pass through, a refreshable connection whose response matches the authorization's refresh-on rules is
 * refreshed here, once, and the request is sent again.
 *
 * <p>
 * The response is requested as binary with {@code bytechef.app.proxy.max-response-bytes} as the client's in-flight
 * limit: the client counts the body while spooling it to temporary file storage and aborts the exchange once the limit
 * is passed, which is answered as {@code APP_PROXY_RESPONSE_TOO_LARGE}. The read-back keeps the same bound as a
 * backstop. Every spooled file, response or request body, is deleted once it is no longer needed; each gets a unique
 * name, because some storage providers key temp files by name alone.
 *
 * <p>
 * Runs synchronously on the calling thread; the caller is responsible for running it on the app lane as the system
 * principal. Bodies, headers and query values are never logged or echoed in error messages.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppProxyExecutor {

    private static final Logger log = LoggerFactory.getLogger(AppProxyExecutor.class);

    private static final int MAX_CAUSE_DEPTH = 16;

    private static final int MAX_CONTENT_TYPE_LENGTH = 256;

    private static final String TOKEN_PUNCTUATION = "!#$%&'*+-.^_`|~";

    private static final Pattern METHOD_PATTERN = Pattern.compile("^[A-Za-z]{1,16}$");

    private static final String OCTET_STREAM = "application/octet-stream";

    private final ApplicationProperties applicationProperties;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ContextFactory contextFactory;
    private final TempFileStorage tempFileStorage;

    @SuppressFBWarnings("EI")
    public AppProxyExecutor(
        ApplicationProperties applicationProperties,
        ConnectionDefinitionService connectionDefinitionService, ContextFactory contextFactory,
        @Qualifier("tempFileStorage") TempFileStorage tempFileStorage) {

        this.applicationProperties = applicationProperties;
        this.connectionDefinitionService = connectionDefinitionService;
        this.contextFactory = contextFactory;
        this.tempFileStorage = tempFileStorage;
    }

    public ProxyResponse execute(AppConnection binding, Connection connection, ProxyRequest request) {
        String method = checkMethodAllowed(binding, request.method());

        if (connection.getAuthorizationType() == null) {
            throw new ConfigurationException(
                "Connection of binding '%s' has no authorization to apply".formatted(binding.getBindingKey()),
                AppErrorType.APP_PROXY_UNSUPPORTED);
        }

        String componentName = connection.getComponentName();

        String path = ProxyPathValidator.validate(request.path());
        Map<String, List<String>> queryParameters = checkQueryParameters(request.query());
        checkHeaders(request.headers());

        String contentType = getContentType(request);
        byte[] bodyBytes = decodeBody(request);

        ApplicationProperties.App.Proxy proxy = applicationProperties.getApp()
            .getProxy();

        if (bodyBytes != null && bodyBytes.length > proxy.getMaxRequestBytes()) {
            throw new ConfigurationException(
                "Proxy request body exceeds %d bytes".formatted(proxy.getMaxRequestBytes()),
                AppErrorType.APP_PROXY_REQUEST_TOO_LARGE);
        }

        Duration timeout = proxy.getTimeout();

        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException(
                "bytechef.app.proxy.timeout must be set to a positive duration, but was " + timeout);
        }

        ComponentConnection componentConnection = new ComponentConnection(
            componentName, connection.getConnectionVersion(), connection.getId(), connection.getParameters(),
            connection.getAuthorizationType());

        Context context = contextFactory.createContext(componentName, componentConnection);

        FileEntry requestSpool = null;

        try {
            Http.Body body;

            if (bodyBytes != null && request.bodyEncoding() == BodyEncoding.BASE64) {
                String requestSpoolName = "proxy-request-" + UUID.randomUUID();

                try {
                    requestSpool = context.file(
                        file -> file.storeContent(requestSpoolName, new ByteArrayInputStream(bodyBytes)));
                } catch (RuntimeException runtimeException) {
                    throw new ProxyStorageException("Unable to store the proxy request body", runtimeException);
                }

                body = Http.Body.of(requestSpool, contentType == null ? OCTET_STREAM : contentType);
            } else {
                body = toTextBody(bodyBytes, contentType);
            }

            Exchange exchange = new Exchange(
                binding.getBindingKey(), method, path, getForwardedHeaders(request.headers()), queryParameters, body,
                timeout, proxy.getMaxResponseBytes());

            UpstreamResponse upstreamResponse = send(exchange, componentConnection, context);

            if (shouldRefresh(componentConnection, upstreamResponse)) {
                componentConnection = refresh(binding, componentConnection);

                context = contextFactory.createContext(componentName, componentConnection);

                upstreamResponse = send(exchange, componentConnection, context);
            }

            return toProxyResponse(upstreamResponse);
        } finally {
            deleteSpool(requestSpool);
        }
    }

    private UpstreamResponse send(Exchange exchange, ComponentConnection componentConnection, Context context) {
        String componentName = componentConnection.getComponentName();

        String baseUri = connectionDefinitionService.executeBaseUri(componentName, componentConnection, context)
            .orElseThrow(() -> new ConfigurationException(
                "Connection of binding '%s' has no base URI to proxy to".formatted(exchange.bindingKey()),
                AppErrorType.APP_PROXY_UNSUPPORTED));

        URI pinnedUri = ProxyPathValidator.pin(baseUri, exchange.path());

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            componentName, componentConnection.version(),
            Objects.requireNonNull(componentConnection.authorizationType()), componentConnection.getParameters(),
            context);

        Map<String, List<String>> headers = new LinkedHashMap<>(exchange.headers());
        Map<String, List<String>> queryParameters = new LinkedHashMap<>(exchange.queryParameters());

        if (applyResponse != null) {
            mergeApplied(headers, applyResponse.getHeaders());
            mergeApplied(queryParameters, applyResponse.getQueryParameters());
        }

        Http.Executor executor = context.http(http -> http.exchange(
            pinnedUri.toString(), Http.RequestMethod.valueOf(exchange.method())));

        executor.headers(headers);
        executor.queryParameters(queryParameters);

        if (exchange.body() != null) {
            executor.body(exchange.body());
        }

        executor.configuration(
            Http.responseType(Http.ResponseType.BINARY)
                .requestTimeout(exchange.timeout())
                .followRedirect(false)
                .followAllRedirects(false)
                .disableAuthorization(true)
                .maxResponseBytes(exchange.maxResponseBytes())
                .filename("proxy-response-" + UUID.randomUUID()));

        Http.Response response;

        try {
            response = executor.execute();
        } catch (RuntimeException runtimeException) {
            if (isResponseTooLarge(runtimeException)) {
                throw responseTooLarge(exchange.maxResponseBytes());
            }

            throw runtimeException;
        }

        int status = response.getStatusCode();
        Map<String, List<String>> responseHeaders = response.getHeaders() == null ? Map.of() : response.getHeaders();

        FileEntry responseSpool = response.getBody(FileEntry.class);

        try {
            byte[] bytes = status == 204 || responseSpool == null
                ? new byte[0] : readBounded(context, responseSpool, exchange.maxResponseBytes());

            return new UpstreamResponse(status, responseHeaders, bytes);
        } finally {
            deleteSpool(responseSpool);
        }
    }

    /**
     * A refresh that fails (the token endpoint refused it, or the connection was marked invalid) leaves the connection
     * unusable. It is reported with a fixed message: the refresh exception's own message can carry the token endpoint's
     * response body. The cause is logged, never returned.
     */
    private ComponentConnection refresh(AppConnection binding, ComponentConnection componentConnection) {
        try {
            return connectionDefinitionService.executeConnectionRefresh(componentConnection);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Credential refresh failed for app {}, binding '{}', connection {}", binding.getAppId(),
                binding.getBindingKey(), componentConnection.getConnectionId(), runtimeException);

            throw new ConfigurationException(
                "Connection of binding '%s' is unavailable".formatted(binding.getBindingKey()), runtimeException,
                AppErrorType.APP_CONNECTION_UNAVAILABLE);
        }
    }

    private boolean shouldRefresh(ComponentConnection componentConnection, UpstreamResponse upstreamResponse) {
        if (!componentConnection.canCredentialsBeRefreshed() || upstreamResponse.status() < 400) {
            return false;
        }

        List<Object> refreshOn = connectionDefinitionService.getAuthorizationRefreshOn(
            componentConnection.getComponentName(), componentConnection.version(),
            Objects.requireNonNull(componentConnection.authorizationType()));

        if (refreshOn == null || refreshOn.isEmpty()) {
            return false;
        }

        String bodyText = upstreamResponse.body().length == 0 ? null : decodeUtf8(upstreamResponse.body());

        return RefreshCredentialsUtils.matches(
            refreshOn, ProviderException.getProviderException(upstreamResponse.status(), bodyText));
    }

    /**
     * Applied credentials must win: any existing entry whose name matches an applied one case-insensitively is removed
     * before the applied entries are added, so an app cannot send its own value alongside the connection's.
     */
    private static void mergeApplied(Map<String, List<String>> target, @Nullable Map<String, List<String>> applied) {
        if (applied == null || applied.isEmpty()) {
            return;
        }

        target.keySet()
            .removeIf(name -> applied.keySet()
                .stream()
                .anyMatch(appliedName -> appliedName.equalsIgnoreCase(name)));

        target.putAll(applied);
    }

    private void deleteSpool(@Nullable FileEntry fileEntry) {
        if (fileEntry == null) {
            return;
        }

        try {
            tempFileStorage.deleteFile(
                new com.bytechef.file.storage.domain.FileEntry(
                    fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl()));
        } catch (RuntimeException runtimeException) {
            log.warn("Unable to delete a proxy spool file", runtimeException);
        }
    }

    private static String checkMethodAllowed(AppConnection binding, @Nullable String requestMethod) {
        if (requestMethod == null || !METHOD_PATTERN.matcher(requestMethod)
            .matches()) {

            throw new ConfigurationException(
                "Proxy method is not a valid HTTP method", AppErrorType.APP_PROXY_REQUEST_INVALID);
        }

        String method = requestMethod.toUpperCase(Locale.ROOT);
        List<String> allowedMethods = binding.isProxyEnabled() ? binding.getProxyAllowedMethodList() : List.of();

        if (!allowedMethods.contains(method)) {
            throw new ConfigurationException(
                "%s is not allowed on binding '%s'; allowed proxy methods: %s".formatted(
                    method, binding.getBindingKey(),
                    allowedMethods.isEmpty() ? "none" : String.join(", ", allowedMethods)),
                AppErrorType.APP_OPERATION_NOT_ALLOWED);
        }

        for (Http.RequestMethod requestMethodValue : Http.RequestMethod.values()) {
            if (requestMethodValue.name()
                .equals(method)) {

                return method;
            }
        }

        throw new ConfigurationException(
            "%s cannot be proxied".formatted(method), AppErrorType.APP_PROXY_UNSUPPORTED);
    }

    private static Map<String, List<String>> checkQueryParameters(Map<String, List<String>> query) {
        Map<String, List<String>> queryParameters = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : query.entrySet()) {
            String name = entry.getKey();

            if (name == null || containsLineBreak(name) || entry.getValue()
                .stream()
                .anyMatch(AppProxyExecutor::containsLineBreak)) {

                throw new ConfigurationException(
                    "Proxy query rejected: parameter names and values must not contain line breaks",
                    AppErrorType.APP_PROXY_PATH_REJECTED);
            }

            queryParameters.put(name, entry.getValue());
        }

        return queryParameters;
    }

    /**
     * Header names must be RFC 9110 tokens and values must not carry control characters (a horizontal tab is allowed).
     * The JDK HTTP client refuses anything else with an exception, which would otherwise surface far from the request
     * that caused it. Names and values are never echoed.
     */
    private static void checkHeaders(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();

            if (name == null || name.isEmpty() || skipToken(name, 0) != name.length()) {
                throw invalidHeader();
            }

            for (String value : entry.getValue()) {
                if (value != null && containsControlCharacter(value)) {
                    throw invalidHeader();
                }
            }
        }
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if ((character < 0x20 && character != '\t') || character == 0x7f) {
                return true;
            }
        }

        return false;
    }

    private static ConfigurationException invalidHeader() {
        return new ConfigurationException(
            "Proxy header rejected: names must be HTTP tokens and values must not contain control characters",
            AppErrorType.APP_PROXY_REQUEST_INVALID);
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private static byte @Nullable [] decodeBody(ProxyRequest request) {
        String body = request.body();

        if (body == null) {
            return null;
        }

        if (request.bodyEncoding() == BodyEncoding.BASE64) {
            try {
                return EncodingUtils.base64Decode(body);
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new ConfigurationException(
                    "Proxy request body is not valid base64", illegalArgumentException,
                    AppErrorType.APP_PROXY_REQUEST_INVALID);
            }
        }

        return body.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * The request's content type, or else the first {@code Content-Type} header the app sent, which must be a
     * well-formed {@code type/subtype} with optional parameters. The body carries it to the client, so the header
     * itself is never forwarded.
     */
    private static @Nullable String getContentType(ProxyRequest request) {
        String contentType = request.contentType();

        if (contentType == null) {
            contentType = getFirstHeader(request.headers(), "content-type");
        }

        if (contentType != null && (contentType.length() > MAX_CONTENT_TYPE_LENGTH
            || !isValidMediaType(contentType.strip()))) {

            throw new ConfigurationException(
                "Proxy content type is not a valid media type", AppErrorType.APP_PROXY_REQUEST_INVALID);
        }

        return contentType == null ? null : contentType.strip();
    }

    /**
     * A strict {@code type/subtype *( OWS ";" OWS token "=" ( token / quoted-string ) )} check (RFC 9110), written as a
     * single forward scan so no input can make it backtrack.
     */
    static boolean isValidMediaType(String value) {
        int length = value.length();
        int index = skipToken(value, 0);

        if (index == 0 || index >= length || value.charAt(index) != '/') {
            return false;
        }

        int subtypeStart = index + 1;

        index = skipToken(value, subtypeStart);

        if (index == subtypeStart) {
            return false;
        }

        while (index < length) {
            index = skipWhitespace(value, index);

            if (index >= length || value.charAt(index) != ';') {
                return false;
            }

            int nameStart = skipWhitespace(value, index + 1);

            index = skipToken(value, nameStart);

            if (index == nameStart || index >= length || value.charAt(index) != '=') {
                return false;
            }

            int valueStart = index + 1;

            if (valueStart < length && value.charAt(valueStart) == '"') {
                index = skipQuotedString(value, valueStart);

                if (index < 0) {
                    return false;
                }
            } else {
                index = skipToken(value, valueStart);

                if (index == valueStart) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isTokenCharacter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
            || (character >= '0' && character <= '9') || TOKEN_PUNCTUATION.indexOf(character) >= 0;
    }

    private static boolean isQuotedTextCharacter(char character) {
        return character == '\t' || (character >= 0x20 && character <= 0x7e);
    }

    /**
     * Returns the index just past the closing quote of the quoted string opening at {@code start}, or {@code -1} when
     * it is unterminated or contains a control character.
     */
    private static int skipQuotedString(String value, int start) {
        int index = start + 1;

        while (index < value.length()) {
            char character = value.charAt(index);

            if (character == '"') {
                return index + 1;
            }

            if (character == '\\') {
                if (index + 1 >= value.length() || !isQuotedTextCharacter(value.charAt(index + 1))) {
                    return -1;
                }

                index += 2;
            } else if (isQuotedTextCharacter(character)) {
                index++;
            } else {
                return -1;
            }
        }

        return -1;
    }

    private static int skipToken(String value, int start) {
        int index = start;

        while (index < value.length() && isTokenCharacter(value.charAt(index))) {
            index++;
        }

        return index;
    }

    private static int skipWhitespace(String value, int start) {
        int index = start;

        while (index < value.length() && (value.charAt(index) == ' ' || value.charAt(index) == '\t')) {
            index++;
        }

        return index;
    }

    private static Map<String, List<String>> getForwardedHeaders(Map<String, List<String>> headers) {
        Map<String, List<String>> forwardedHeaders = ProxyHeaders.sanitizeRequest(headers);

        forwardedHeaders.keySet()
            .removeIf(name -> name.equalsIgnoreCase("content-type"));

        return forwardedHeaders;
    }

    private static @Nullable String getFirstHeader(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && !entry.getValue()
                .isEmpty()) {

                return entry.getValue()
                    .getFirst();
            }
        }

        return null;
    }

    private static boolean isText(@Nullable String contentType) {
        if (contentType == null) {
            return false;
        }

        String mediaType = contentType.split(";", 2)[0].strip()
            .toLowerCase(Locale.ROOT);

        return mediaType.startsWith("text/") || mediaType.equals("application/json")
            || mediaType.equals("application/xml") || mediaType.equals("application/x-www-form-urlencoded")
            || mediaType.endsWith("+json") || mediaType.endsWith("+xml");
    }

    private static byte[] readBounded(Context context, FileEntry fileEntry, long maxResponseBytes) {
        try {
            long contentLength = context.file(file -> file.getContentLength(fileEntry));

            if (contentLength > maxResponseBytes) {
                throw responseTooLarge(maxResponseBytes);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (InputStream inputStream = context.file(file -> file.getInputStream(fileEntry))) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    total += read;

                    if (total > maxResponseBytes) {
                        throw responseTooLarge(maxResponseBytes);
                    }

                    outputStream.write(buffer, 0, read);
                }
            }

            return outputStream.toByteArray();
        } catch (ConfigurationException configurationException) {
            throw configurationException;
        } catch (IOException | RuntimeException exception) {
            throw new ProxyStorageException("Unable to read the proxied response body", exception);
        }
    }

    private static ConfigurationException responseTooLarge(long maxResponseBytes) {
        return new ConfigurationException(
            "Proxy response body exceeds %d bytes".formatted(maxResponseBytes),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);
    }

    private static boolean isResponseTooLarge(Throwable throwable) {
        Throwable cause = throwable;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof HttpResponseTooLargeException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private static Http.@Nullable Body toTextBody(byte @Nullable [] bodyBytes, @Nullable String contentType) {
        if (bodyBytes == null) {
            return null;
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        return contentType == null ? Http.Body.of(body) : Http.Body.of(body, contentType);
    }

    private static ProxyResponse toProxyResponse(UpstreamResponse upstreamResponse) {
        int status = upstreamResponse.status();
        Map<String, List<String>> headers = upstreamResponse.headers();
        Map<String, List<String>> sanitizedHeaders = ProxyHeaders.sanitizeResponse(headers);
        byte[] bytes = upstreamResponse.body();

        if (bytes.length == 0) {
            return new ProxyResponse(status, sanitizedHeaders, null, BodyEncoding.UTF8);
        }

        if (isText(getFirstHeader(headers, "content-type"))) {
            String text = decodeUtf8(bytes);

            if (text != null) {
                return new ProxyResponse(status, sanitizedHeaders, text, BodyEncoding.UTF8);
            }
        }

        return new ProxyResponse(
            status, sanitizedHeaders, EncodingUtils.base64EncodeToString(bytes), BodyEncoding.BASE64);
    }

    private static @Nullable String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException characterCodingException) {
            return null;
        }
    }

    /**
     * Everything about one proxied request that stays the same when it is sent again after a token refresh.
     */
    private record Exchange(
        String bindingKey, String method, String path, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters, Http.@Nullable Body body, Duration timeout,
        long maxResponseBytes) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    private record UpstreamResponse(int status, Map<String, List<String>> headers, byte[] body) {
    }
}
