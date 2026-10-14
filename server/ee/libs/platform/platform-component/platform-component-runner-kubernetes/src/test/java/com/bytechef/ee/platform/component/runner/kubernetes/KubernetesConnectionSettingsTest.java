/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.component.kubernetes.constant.KubernetesConstants;
import com.bytechef.platform.component.ComponentConnection;
import io.fabric8.kubernetes.client.Config;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class KubernetesConnectionSettingsTest {

    private static final String PEM =
        "-----BEGIN CERTIFICATE-----\nMIIBszCCAVmgAwIBAgIBADAK\n-----END CERTIFICATE-----\n";

    @Test
    void testEveryFieldIsReadFromTheConnection() {
        KubernetesConnectionSettings settings = KubernetesConnectionSettings.of(
            connection(
                Map.of(
                    "masterUrl", "https://cluster:6443", "caCertData", PEM, "token", "secret-token", "namespace",
                    "jobs", "insecureSkipTlsVerify", true)));

        assertThat(settings.masterUrl()).isEqualTo("https://cluster:6443");
        assertThat(settings.caCertData()).isEqualTo(PEM);
        assertThat(settings.token()).isEqualTo("secret-token");
        assertThat(settings.namespace()).isEqualTo("jobs");
        assertThat(settings.insecureSkipTlsVerify()).isTrue();
    }

    @Test
    void testABlankNamespaceFallsBackToDefault() {
        KubernetesConnectionSettings settings = KubernetesConnectionSettings.of(
            connection(Map.of("masterUrl", "https://cluster:6443", "token", "t", "namespace", " ")));

        assertThat(settings.namespace()).isEqualTo("default");
    }

    @Test
    void testValidateRequiresAnHttpApiServerUrl() {
        assertThatThrownBy(() -> settings(Map.of("token", "t")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API server URL");

        assertThatThrownBy(() -> settings(Map.of("masterUrl", "cluster:6443", "token", "t")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("https://");
    }

    @Test
    void testValidateRequiresATokenOrAClientCertificateWithItsKey() {
        assertThatThrownBy(() -> settings(Map.of("masterUrl", "https://cluster:6443")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("token or a client certificate");

        assertThatThrownBy(
            () -> settings(Map.of("masterUrl", "https://cluster:6443", "clientCertData", PEM)).validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client key");

        settings(Map.of("masterUrl", "https://cluster:6443", "token", "t")).validate();
        settings(Map.of("masterUrl", "https://cluster:6443", "clientCertData", PEM, "clientKeyData", PEM)).validate();
    }

    @Test
    void testValidateRejectsABearerTokenOverPlainHttp() {
        assertThatThrownBy(() -> settings(Map.of("masterUrl", "http://cluster:8080", "token", "t")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("clear text");

        settings(Map.of("masterUrl", "http://cluster:8080", "clientCertData", PEM, "clientKeyData", PEM)).validate();
    }

    @Test
    void testAPemCertificateIsBase64EncodedForTheClient() {
        Config config = settings(
            Map.of("masterUrl", "https://cluster:6443", "token", "t", "caCertData", PEM.strip() + "  \r\n\n"))
                .toConfig(Duration.ofSeconds(30));

        assertThat(config.getCaCertData()).isEqualTo(EncodingUtils.base64EncodeToString(PEM));
    }

    @Test
    void testAPemCertificateWithALeadingNewlineIsRecognised() {
        KubernetesConnectionSettings settings = settings(
            Map.of("masterUrl", "https://cluster:6443", "token", "t", "caCertData", "\n  " + PEM));

        assertThat(settings.caCertData()).isEqualTo(PEM);
        assertThat(settings.toConfig(Duration.ofSeconds(30))
            .getCaCertData()).isEqualTo(EncodingUtils.base64EncodeToString(PEM));
    }

    @Test
    void testABase64CertificatePassesThroughUnchanged() {
        String base64 = EncodingUtils.base64EncodeToString(PEM);

        Config config = settings(Map.of("masterUrl", "https://cluster:6443", "token", "t", "caCertData", base64))
            .toConfig(Duration.ofSeconds(30));

        assertThat(config.getCaCertData()).isEqualTo(base64);
    }

    @Test
    void testAnEllipticCurveClientKeyIsLoadedAsEc() {
        String ecKey = "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIA\n-----END EC PRIVATE KEY-----\n";

        Config config = settings(
            Map.of("masterUrl", "https://cluster:6443", "clientCertData", PEM, "clientKeyData", ecKey))
                .toConfig(Duration.ofSeconds(30));

        assertThat(config.getClientKeyAlgo()).isEqualTo("EC");
    }

    @Test
    void testTheConfigCarriesTheConnectionAndNothingFromTheAmbientKubeconfig() {
        Config config = settings(
            Map.of(
                "masterUrl", "https://cluster:6443", "token", "secret-token", "namespace", "jobs",
                "insecureSkipTlsVerify", true))
                    .toConfig(Duration.ofSeconds(30));

        assertThat(config.getMasterUrl()).startsWith("https://cluster:6443");
        assertThat(config.getOauthToken()).isEqualTo("secret-token");
        assertThat(config.getNamespace()).isEqualTo("jobs");
        assertThat(config.isTrustCerts()).isTrue();
        assertThat(config.getRequestTimeout()).isEqualTo(30_000);
        assertThat(config.getCurrentContext()).isNull();
    }

    @Test
    void testToStringNeverShowsTheTokenOrTheClientKey() {
        String text = settings(
            Map.of(
                "masterUrl", "https://cluster:6443", "token", "secret-token", "clientCertData", PEM, "clientKeyData",
                "secret-key")).toString();

        assertThat(text).doesNotContain("secret-token")
            .doesNotContain("secret-key")
            .contains("https://cluster:6443");
    }

    @Test
    void testTheConnectionComponentDeclaresEveryParsedField() {
        assertThat(KubernetesConnectionSettings.MASTER_URL).isEqualTo(KubernetesConstants.MASTER_URL);
        assertThat(KubernetesConnectionSettings.CA_CERT_DATA).isEqualTo(KubernetesConstants.CA_CERT_DATA);
        assertThat(KubernetesConnectionSettings.TOKEN).isEqualTo(KubernetesConstants.TOKEN);
        assertThat(KubernetesConnectionSettings.CLIENT_CERT_DATA).isEqualTo(KubernetesConstants.CLIENT_CERT_DATA);
        assertThat(KubernetesConnectionSettings.CLIENT_KEY_DATA).isEqualTo(KubernetesConstants.CLIENT_KEY_DATA);
        assertThat(KubernetesConnectionSettings.NAMESPACE).isEqualTo(KubernetesConstants.NAMESPACE);
        assertThat(KubernetesConnectionSettings.INSECURE_SKIP_TLS_VERIFY)
            .isEqualTo(KubernetesConstants.INSECURE_SKIP_TLS_VERIFY);
    }

    private static ComponentConnection connection(Map<String, ?> parameters) {
        return new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(parameters), null);
    }

    private static KubernetesConnectionSettings settings(Map<String, ?> parameters) {
        return KubernetesConnectionSettings.of(connection(parameters));
    }
}
