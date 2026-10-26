/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway;

import com.bytechef.commons.util.EncodingUtils;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Talks to the gateway over HTTP the way the browser does: keeps cookies and sends the CSRF token from the
 * {@code XSRF-TOKEN} cookie. Like the client, it reads the account right after login to pick up the rotated CSRF token.
 *
 * @version ee
 */
class GatewayBrowser {

    private final String baseUrl;
    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient httpClient;

    GatewayBrowser(int port) {
        this.baseUrl = "http://localhost:" + port;
        this.httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    void addCookie(String name, String value) {
        HttpCookie httpCookie = new HttpCookie(name, value);

        httpCookie.setPath("/");
        httpCookie.setVersion(0);

        cookieManager.getCookieStore()
            .add(URI.create(baseUrl), httpCookie);
    }

    Optional<String> cookie(String name) {
        return cookieManager.getCookieStore()
            .getCookies()
            .stream()
            .filter(httpCookie -> name.equals(httpCookie.getName()))
            .map(HttpCookie::getValue)
            .findFirst();
    }

    HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
            .GET());
    }

    HttpResponse<String> login(String username, String password, boolean rememberMe)
        throws IOException, InterruptedException {

        get("/api/account");

        Map<String, String> form = rememberMe
            ? Map.of("username", username, "password", password, "remember-me", "true")
            : Map.of("username", username, "password", password);

        HttpResponse<String> response = postForm("/api/authentication", form, true);

        get("/api/account");

        return response;
    }

    HttpResponse<String> postForm(String path, Map<String, String> form, boolean withCsrfToken)
        throws IOException, InterruptedException {

        String body = form.entrySet()
            .stream()
            .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body));

        return send(withCsrfToken ? withCsrfToken(builder) : builder);
    }

    HttpResponse<String> postJson(String path, String json, Map<String, String> headers, boolean withCsrfToken)
        throws IOException, InterruptedException {

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));

        headers.forEach(builder::header);

        return send(withCsrfToken ? withCsrfToken(builder) : builder);
    }

    void removeCookie(String name) {
        List<HttpCookie> httpCookies = cookieManager.getCookieStore()
            .getCookies()
            .stream()
            .filter(httpCookie -> name.equals(httpCookie.getName()))
            .toList();

        for (HttpCookie httpCookie : httpCookies) {
            cookieManager.getCookieStore()
                .remove(URI.create(baseUrl), httpCookie);
        }
    }

    Optional<String> sessionId() {
        return cookie("SESSION").map(EncodingUtils::base64DecodeToString);
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder withCsrfToken(HttpRequest.Builder builder) {
        return builder.header("X-XSRF-TOKEN", cookie("XSRF-TOKEN").orElse(""));
    }
}
