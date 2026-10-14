/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.ComponentConnection;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import java.time.Duration;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The cluster a Kubernetes task runner talks to, read from the task's {@code kubernetes} connection.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record KubernetesConnectionSettings(
    String masterUrl, @Nullable String caCertData, @Nullable String token, @Nullable String clientCertData,
    @Nullable String clientKeyData, String namespace, boolean insecureSkipTlsVerify) {

    static final String CA_CERT_DATA = "caCertData";
    static final String CLIENT_CERT_DATA = "clientCertData";
    static final String CLIENT_KEY_DATA = "clientKeyData";
    static final String INSECURE_SKIP_TLS_VERIFY = "insecureSkipTlsVerify";
    static final String MASTER_URL = "masterUrl";
    static final String NAMESPACE = "namespace";
    static final String TOKEN = "token";

    private static final String DEFAULT_NAMESPACE = "default";
    private static final String PEM_PREFIX = "-----BEGIN";

    public static KubernetesConnectionSettings of(ComponentConnection connection) {
        Map<String, ?> parameters = connection.getParameters();

        String namespace = text(parameters, NAMESPACE);
        String masterUrl = text(parameters, MASTER_URL);

        return new KubernetesConnectionSettings(
            masterUrl == null ? "" : masterUrl,
            pemText(parameters, CA_CERT_DATA), text(parameters, TOKEN), pemText(parameters, CLIENT_CERT_DATA),
            pemText(parameters, CLIENT_KEY_DATA), namespace == null ? DEFAULT_NAMESPACE : namespace,
            Boolean.parseBoolean(String.valueOf(parameters.get(INSECURE_SKIP_TLS_VERIFY))));
    }

    public void validate() {
        if (masterUrl.isEmpty()) {
            throw new IllegalArgumentException("The kubernetes connection needs the cluster's API server URL");
        }

        if (!masterUrl.startsWith("https://") && !masterUrl.startsWith("http://")) {
            throw new IllegalArgumentException(
                "The kubernetes connection's API server URL must start with https:// (or http://)");
        }

        if (token != null && masterUrl.startsWith("http://")) {
            throw new IllegalArgumentException(
                "The kubernetes connection's API server URL must use https:// with a bearer token; http:// would " +
                    "send the token in clear text");
        }

        if (clientCertData != null && clientKeyData == null) {
            throw new IllegalArgumentException(
                "The kubernetes connection has a client certificate but no client key");
        }

        if (token == null && clientCertData == null) {
            throw new IllegalArgumentException(
                "The kubernetes connection needs a bearer token or a client certificate and key");
        }
    }

    public Config toConfig(Duration requestTimeout) {
        ConfigBuilder configBuilder = new ConfigBuilder(Config.empty());

        configBuilder.withMasterUrl(masterUrl);
        configBuilder.withNamespace(namespace);
        configBuilder.withTrustCerts(insecureSkipTlsVerify);
        configBuilder.withDisableHostnameVerification(insecureSkipTlsVerify);
        configBuilder.withRequestTimeout((int) requestTimeout.toMillis());
        configBuilder.withConnectionTimeout((int) Math.min(requestTimeout.toMillis(), 30_000));

        if (caCertData != null) {
            configBuilder.withCaCertData(toBase64Pem(caCertData));
        }

        if (token != null) {
            configBuilder.withOauthToken(token);
        }

        if (clientCertData != null && clientKeyData != null) {
            String encodedClientKeyData = toBase64Pem(clientKeyData);

            configBuilder.withClientCertData(toBase64Pem(clientCertData));
            configBuilder.withClientKeyData(encodedClientKeyData);
            configBuilder.withClientKeyAlgo(Config.getKeyAlgorithm(null, encodedClientKeyData));
        }

        return configBuilder.build();
    }

    @Override
    public String toString() {
        return "KubernetesConnectionSettings[masterUrl=%s, namespace=%s, token=%s, clientKey=%s, insecure=%s]"
            .formatted(
                masterUrl, namespace, token == null ? "none" : "***", clientKeyData == null ? "none" : "***",
                insecureSkipTlsVerify);
    }

    private static String toBase64Pem(String value) {
        return value.startsWith(PEM_PREFIX) ? EncodingUtils.base64EncodeToString(value) : value;
    }

    @Nullable
    private static String text(Map<String, ?> parameters, String name) {
        Object value = parameters.get(name);

        if (value == null) {
            return null;
        }

        String trimmed = value.toString()
            .trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    private static String pemText(Map<String, ?> parameters, String name) {
        Object value = parameters.get(name);

        if (value == null) {
            return null;
        }

        String stripped = value.toString()
            .strip();

        if (stripped.isEmpty()) {
            return null;
        }

        return stripped.startsWith(PEM_PREFIX) ? stripped + "\n" : stripped;
    }
}
