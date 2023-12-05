package com.bytechef.component.googledrive.util;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.Preconditions;

import java.io.IOException;

public class OAuthAuthentication implements HttpRequestInitializer, HttpExecuteInterceptor {

    private final String token;

    public OAuthAuthentication(String token) {
        this.token = Preconditions.checkNotNull(token);
    }

    public void initialize(HttpRequest request) throws IOException {
        request.setInterceptor(this);
    }

    public void intercept(HttpRequest request) throws IOException {
        request.getHeaders().set("Authorization", "Bearer " + token);
    }
}
