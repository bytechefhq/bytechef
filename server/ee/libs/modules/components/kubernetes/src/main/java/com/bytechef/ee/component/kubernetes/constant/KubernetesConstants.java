/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.kubernetes.constant;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class KubernetesConstants {

    public static final String CA_CERT_DATA = "caCertData";
    public static final String CLIENT_CERT_DATA = "clientCertData";
    public static final String CLIENT_KEY_DATA = "clientKeyData";
    public static final String INSECURE_SKIP_TLS_VERIFY = "insecureSkipTlsVerify";
    public static final String MASTER_URL = "masterUrl";
    public static final String NAMESPACE = "namespace";
    public static final String TOKEN = "token";

    private KubernetesConstants() {
    }
}
