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

package com.bytechef.component.neon.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.HEADER_PREFIX;
import static com.bytechef.component.definition.Authorization.SCOPES;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN_URL;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.neon.constant.NeonConstants.KEY_ID;
import static com.bytechef.component.neon.constant.NeonConstants.PRIVATE_KEY;
import static com.bytechef.component.neon.constant.NeonConstants.SUBJECT;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeonConnection {

    private static final String AUDIENCE = "audience";
    private static final String EXPIRATION_SECONDS = "expirationSeconds";

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .version(1)
        .help("", "https://docs.bytechef.io/reference/components/neon_v1#connection-setup")
        .baseUri((connectionParameters, context) -> {
            String baseUri = connectionParameters.getRequiredString(BASE_URI);

            return baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri;
        })
        .properties(
            string(BASE_URI)
                .label("Data API URL")
                .description(
                    "The Data API endpoint for your Neon branch, found in the Neon Console under Postgres " +
                        "database > Data API.")
                .exampleValue("https://ep-example.apirest.us-east-1.aws.neon.tech/neondb/rest/v1")
                .required(true))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .title("Self-Signed JWT")
                .description(
                    "Signs a fresh RS256 JWT locally before every request, using a private key you control. " +
                        "Register the matching public key with Neon as a custom auth provider's JWKS URL. This " +
                        "avoids depending on a third-party auth provider and never hits an external token " +
                        "endpoint.")
                .properties(
                    string(PRIVATE_KEY)
                        .label("Private Key")
                        .description(
                            "PKCS#8 PEM-encoded RSA private key, e.g. generated with " +
                                "\"openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048\".")
                        .controlType(ControlType.TEXT_AREA)
                        .required(true),
                    string(SUBJECT)
                        .label("Subject")
                        .description(
                            "Value for the JWT's \"sub\" claim. Row-Level Security policies typically use this " +
                                "via auth.user_id(), so it must match what your policies expect.")
                        .required(true),
                    string(KEY_ID)
                        .label("Key Id")
                        .description(
                            "Value for the JWT header's \"kid\" claim. Must match the key id used in the JWKS " +
                                "document you publish for Neon to pick the right public key.")
                        .required(false),
                    string(AUDIENCE)
                        .label("Audience")
                        .description("Optional value for the JWT's \"aud\" claim.")
                        .required(false),
                    integer(EXPIRATION_SECONDS)
                        .label("Expiration (Seconds)")
                        .description("How long each freshly signed token is valid for.")
                        .defaultValue(300)
                        .required(false))
                .apply(NeonConnection::applySelfSignedJwt),
            authorization(AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                .title("OAuth2 Client Credentials")
                .properties(
                    string(TOKEN_URL)
                        .label("Token URL")
                        .description(
                            "The token endpoint of the auth provider configured for this Neon project (e.g. an " +
                                "Auth0 or Clerk machine-to-machine application).")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(AUDIENCE)
                        .label("Audience")
                        .description(
                            "Required by some providers, e.g. Auth0, to get back a JWT instead of an opaque " +
                                "token. Set it to the API identifier registered with the provider.")
                        .required(false),
                    string(HEADER_PREFIX)
                        .label("Header Prefix")
                        .defaultValue(Authorization.BEARER),
                    string(SCOPES)
                        .label("Scopes")
                        .description("Optional comma-delimited list of scopes.")
                        .controlType(ControlType.TEXT_AREA))
                .apply(NeonConnection::applyOAuth2ClientCredentials),
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("JWT Token")
                        .description(
                            "A JWT bearer token issued by your Neon Auth provider. Session JWTs typically " +
                                "expire after about 15 minutes, so this option is best for quick testing rather " +
                                "than scheduled workflows.")
                        .required(true)));

    private NeonConnection() {
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ApplyResponse applySelfSignedJwt(Parameters connectionParameters, Context context)
        throws Exception {

        RSAPrivateKey privateKey = parseRsaPrivateKey(connectionParameters.getRequiredString(PRIVATE_KEY));

        Algorithm algorithm = Algorithm.RSA256(null, privateKey);

        Instant issuedAt = Instant.now();

        JWTCreator.Builder builder = JWT.create()
            .withSubject(connectionParameters.getRequiredString(SUBJECT))
            .withIssuedAt(issuedAt)
            .withExpiresAt(issuedAt.plusSeconds(connectionParameters.getInteger(EXPIRATION_SECONDS, 300)));

        String keyId = connectionParameters.getString(KEY_ID);

        if (keyId != null && !keyId.isBlank()) {
            builder.withKeyId(keyId);
        }

        String audience = connectionParameters.getString(AUDIENCE);

        if (audience != null && !audience.isBlank()) {
            builder.withAudience(audience);
        }

        return ApplyResponse.ofHeaders(
            Map.of(AUTHORIZATION, List.of(Authorization.BEARER + " " + builder.sign(algorithm))));
    }

    private static RSAPrivateKey parseRsaPrivateKey(String pem) throws Exception {
        String sanitizedPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) keyFactory.generatePrivate(
            new PKCS8EncodedKeySpec(Base64.getDecoder()
                .decode(sanitizedPem)));
    }

    @SuppressWarnings("unchecked")
    private static ApplyResponse applyOAuth2ClientCredentials(Parameters connectionParameters, Context context)
        throws Exception {

        Map<String, String> formParameters = new LinkedHashMap<>();

        formParameters.put("grant_type", "client_credentials");

        String scopes = connectionParameters.getString(SCOPES);

        if (scopes != null && !scopes.isBlank()) {
            formParameters.put("scope", scopes.replace(",", " ")
                .trim());
        }

        String audience = connectionParameters.getString(AUDIENCE);

        if (audience != null && !audience.isBlank()) {
            formParameters.put(AUDIENCE, audience.trim());
        }

        String basicCredentials = context.encoder(
            encoder -> encoder.base64Encode(
                connectionParameters.getRequiredString(CLIENT_ID), ":",
                connectionParameters.getRequiredString(CLIENT_SECRET)));

        Http.Response tokenResponse = context
            .http(http -> http.post(connectionParameters.getRequiredString(TOKEN_URL)))
            .body(Http.Body.of(formParameters, Http.BodyContentType.FORM_URL_ENCODED))
            .header(AUTHORIZATION, "Basic " + basicCredentials)
            .configuration(
                Http.responseType(Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();

        Map<String, ?> responseBody = tokenResponse.getBody(Map.class);

        Object accessTokenObject = responseBody == null ? null : responseBody.get(ACCESS_TOKEN);

        if (!(accessTokenObject instanceof String accessToken) || accessToken.isBlank()) {
            throw new IllegalStateException("OAuth provider did not return an access token");
        }

        String headerPrefix = connectionParameters.getString(HEADER_PREFIX, Authorization.BEARER);

        return ApplyResponse.ofHeaders(Map.of(AUTHORIZATION, List.of(headerPrefix + " " + accessToken)));
    }
}
