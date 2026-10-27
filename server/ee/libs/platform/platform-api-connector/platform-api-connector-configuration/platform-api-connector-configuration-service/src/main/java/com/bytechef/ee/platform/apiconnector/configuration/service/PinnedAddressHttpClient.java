/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.jspecify.annotations.Nullable;

/**
 * HTTP/1.1 GET client that connects only to the given validated addresses and never resolves the host itself.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PinnedAddressHttpClient {

    private static final int MAX_HEADER_COUNT = 200;
    private static final int MAX_LINE_LENGTH = 16 * 1024;

    private final int connectTimeoutMillis;
    private final int maxBodyBytes;
    private final int readTimeoutMillis;
    private final SSLSocketFactory sslSocketFactory;

    PinnedAddressHttpClient(
        Duration connectTimeout, Duration readTimeout, int maxBodyBytes, SSLSocketFactory sslSocketFactory) {

        this.connectTimeoutMillis = Math.toIntExact(connectTimeout.toMillis());
        this.maxBodyBytes = maxBodyBytes;
        this.readTimeoutMillis = Math.toIntExact(readTimeout.toMillis());
        this.sslSocketFactory = sslSocketFactory;
    }

    Response get(URI uri, List<InetAddress> addresses, Map<String, String> headers) throws IOException {
        String scheme = uri.getScheme()
            .toLowerCase(Locale.ROOT);
        boolean secure = scheme.equals("https");
        int port = uri.getPort() == -1 ? (secure ? 443 : 80) : uri.getPort();

        try (Socket socket = openSocket(uri, addresses, port, secure)) {
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(buildRequest(uri, headers).getBytes(StandardCharsets.ISO_8859_1));

            outputStream.flush();

            return readResponse(new BufferedInputStream(socket.getInputStream()));
        }
    }

    private Socket openSocket(URI uri, List<InetAddress> addresses, int port, boolean secure) throws IOException {
        Socket socket = connect(addresses, port);

        if (!secure) {
            return socket;
        }

        String host = getBareHost(uri);

        try {
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, host, port, true);

            SSLParameters sslParameters = sslSocket.getSSLParameters();

            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

            if (!isIpLiteral(host)) {
                sslParameters.setServerNames(List.of(new SNIHostName(host)));
            }

            sslSocket.setSSLParameters(sslParameters);

            sslSocket.startHandshake();

            return sslSocket;
        } catch (IOException | RuntimeException exception) {
            socket.close();

            throw exception;
        }
    }

    @SuppressFBWarnings(value = "UNENCRYPTED_SOCKET", justification = "Plain HTTP documentation URLs are allowed")
    private Socket connect(List<InetAddress> addresses, int port) throws IOException {
        IOException lastException = new IOException("No address to connect to");

        for (InetAddress address : addresses) {
            Socket socket = new Socket();

            try {
                socket.connect(new InetSocketAddress(address, port), connectTimeoutMillis);

                socket.setSoTimeout(readTimeoutMillis);

                return socket;
            } catch (IOException ioException) {
                socket.close();

                lastException = ioException;
            }
        }

        throw lastException;
    }

    private static String buildRequest(URI uri, Map<String, String> headers) {
        String path = uri.getRawPath();

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        String query = uri.getRawQuery();

        if (query != null) {
            path = path + "?" + query;
        }

        String hostHeader = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();

        StringBuilder request = new StringBuilder();

        request.append("GET ")
            .append(path)
            .append(" HTTP/1.1\r\n");
        request.append("Host: ")
            .append(hostHeader)
            .append("\r\n");

        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.append(header.getKey())
                .append(": ")
                .append(header.getValue())
                .append("\r\n");
        }

        request.append("Accept-Encoding: identity\r\n");
        request.append("Connection: close\r\n\r\n");

        return request.toString();
    }

    private Response readResponse(InputStream inputStream) throws IOException {
        int statusCode;
        Map<String, List<String>> headers;

        do {
            statusCode = parseStatusCode(readLine(inputStream));
            headers = readHeaders(inputStream);
        } while (statusCode >= 100 && statusCode < 200);

        byte[] body = readBody(inputStream, headers);

        Optional<String> location = firstValue(headers, "location");

        return new Response(statusCode, location.orElse(null), new String(body, getCharset(headers)));
    }

    private static int parseStatusCode(String statusLine) throws IOException {
        String[] parts = statusLine.split(" ", 3);

        if (parts.length < 2 || !parts[0].startsWith("HTTP/")) {
            throw new IOException("Malformed HTTP status line: " + statusLine);
        }

        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException numberFormatException) {
            throw new IOException("Malformed HTTP status line: " + statusLine, numberFormatException);
        }
    }

    private static Map<String, List<String>> readHeaders(InputStream inputStream) throws IOException {
        Map<String, List<String>> headers = new LinkedHashMap<>();

        for (int headerCount = 0; headerCount <= MAX_HEADER_COUNT; headerCount++) {
            String line = readLine(inputStream);

            if (line.isEmpty()) {
                return headers;
            }

            int separatorIndex = line.indexOf(':');

            if (separatorIndex <= 0) {
                throw new IOException("Malformed HTTP header: " + line);
            }

            String name = line.substring(0, separatorIndex)
                .trim()
                .toLowerCase(Locale.ROOT);
            String value = line.substring(separatorIndex + 1)
                .trim();

            headers.computeIfAbsent(name, key -> new ArrayList<>())
                .add(value);
        }

        throw new IOException("Too many HTTP response headers");
    }

    private byte[] readBody(InputStream inputStream, Map<String, List<String>> headers) throws IOException {
        Optional<String> transferEncoding = firstValue(headers, "transfer-encoding");

        if (transferEncoding.isPresent() && transferEncoding.get()
            .toLowerCase(Locale.ROOT)
            .contains("chunked")) {

            return readChunkedBody(inputStream);
        }

        Optional<String> contentLength = firstValue(headers, "content-length");

        if (contentLength.isPresent()) {
            long length;

            try {
                length = Long.parseLong(contentLength.get());
            } catch (NumberFormatException numberFormatException) {
                throw new IOException("Malformed Content-Length: " + contentLength.get(), numberFormatException);
            }

            if (length < 0 || length > maxBodyBytes) {
                throw new IOException("Response body exceeds " + maxBodyBytes + " bytes");
            }

            return readFixedLength(inputStream, (int) length);
        }

        return readUntilEndOfStream(inputStream);
    }

    private byte[] readChunkedBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        while (true) {
            String sizeLine = readLine(inputStream);

            int extensionIndex = sizeLine.indexOf(';');

            String sizeText = (extensionIndex >= 0 ? sizeLine.substring(0, extensionIndex) : sizeLine).trim();

            int chunkSize;

            try {
                chunkSize = Integer.parseInt(sizeText, 16);
            } catch (NumberFormatException numberFormatException) {
                throw new IOException("Malformed chunk size: " + sizeLine, numberFormatException);
            }

            if (chunkSize < 0 || body.size() + (long) chunkSize > maxBodyBytes) {
                throw new IOException("Response body exceeds " + maxBodyBytes + " bytes");
            }

            if (chunkSize == 0) {
                readHeaders(inputStream);

                return body.toByteArray();
            }

            body.write(readFixedLength(inputStream, chunkSize));

            readLine(inputStream);
        }
    }

    private static byte[] readFixedLength(InputStream inputStream, int length) throws IOException {
        byte[] bytes = inputStream.readNBytes(length);

        if (bytes.length != length) {
            throw new IOException("Unexpected end of HTTP response body");
        }

        return bytes;
    }

    private byte[] readUntilEndOfStream(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readNBytes(maxBodyBytes + 1);

        if (bytes.length > maxBodyBytes) {
            throw new IOException("Response body exceeds " + maxBodyBytes + " bytes");
        }

        return bytes;
    }

    private static String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();

        while (true) {
            int value = inputStream.read();

            if (value == -1) {
                throw new IOException("Unexpected end of HTTP response");
            }

            if (value == '\n') {
                byte[] bytes = line.toByteArray();

                int length = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;

                return new String(bytes, 0, length, StandardCharsets.ISO_8859_1);
            }

            if (line.size() >= MAX_LINE_LENGTH) {
                throw new IOException("HTTP response line is too long");
            }

            line.write(value);
        }
    }

    private static Charset getCharset(Map<String, List<String>> headers) {
        Optional<String> contentType = firstValue(headers, "content-type");

        if (contentType.isEmpty()) {
            return StandardCharsets.UTF_8;
        }

        for (String parameter : contentType.get()
            .split(";")) {

            String trimmedParameter = parameter.trim();

            if (trimmedParameter.toLowerCase(Locale.ROOT)
                .startsWith("charset=")) {

                String charsetName = trimmedParameter.substring("charset=".length())
                    .replace("\"", "")
                    .trim();

                try {
                    return Charset.forName(charsetName);
                } catch (IllegalCharsetNameException | UnsupportedCharsetException exception) {
                    return StandardCharsets.UTF_8;
                }
            }
        }

        return StandardCharsets.UTF_8;
    }

    private static Optional<String> firstValue(Map<String, List<String>> headers, String name) {
        List<String> values = headers.get(name);

        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(values.getFirst());
    }

    private static String getBareHost(URI uri) {
        String host = uri.getHost();

        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }

        return host;
    }

    private static boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    record Response(int statusCode, @Nullable String location, String body) {
    }
}
