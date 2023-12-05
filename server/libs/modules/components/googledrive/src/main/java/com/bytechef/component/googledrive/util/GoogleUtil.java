package com.bytechef.component.googledrive.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequest;

import java.io.IOException;

public class GoogleUtil {

    public static Credential getCredential(String accessToken){
        return new Credential(new Credential.AccessMethod() {
            @Override
            public void intercept(HttpRequest request, String accessToken) throws IOException {
                System.out.println("");
            }

            @Override
            public String getAccessTokenFromRequest(HttpRequest request) {
                return accessToken;
            }
        });
    }
}
