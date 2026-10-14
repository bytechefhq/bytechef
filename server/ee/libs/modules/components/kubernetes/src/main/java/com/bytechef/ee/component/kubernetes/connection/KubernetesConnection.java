/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.kubernetes.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.CA_CERT_DATA;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.CLIENT_CERT_DATA;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.CLIENT_KEY_DATA;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.INSECURE_SKIP_TLS_VERIFY;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.MASTER_URL;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.NAMESPACE;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.TOKEN;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class KubernetesConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .title("Cluster Credentials")
                .properties(
                    string(MASTER_URL)
                        .label("API Server URL")
                        .description("The cluster's API server, for example https://my-cluster.example.com:6443.")
                        .required(true),
                    string(CA_CERT_DATA)
                        .label("CA Certificate")
                        .description(
                            "The certificate authority that signed the API server's certificate, as PEM or base64 " +
                                "PEM. Leave blank when the API server uses a publicly trusted certificate.")
                        .controlType(ControlType.TEXT_AREA)
                        .required(false),
                    string(TOKEN)
                        .label("Bearer Token")
                        .description(
                            "A service account token. Give either a token or a client certificate and key.")
                        .controlType(ControlType.PASSWORD)
                        .required(false),
                    string(CLIENT_CERT_DATA)
                        .label("Client Certificate")
                        .description("The client certificate, as PEM or base64 PEM.")
                        .controlType(ControlType.TEXT_AREA)
                        .required(false),
                    string(CLIENT_KEY_DATA)
                        .label("Client Key")
                        .description("The client certificate's private key, as PEM or base64 PEM.")
                        .controlType(ControlType.PASSWORD)
                        .required(false),
                    string(NAMESPACE)
                        .label("Namespace")
                        .description("The namespace jobs run in unless a task names another.")
                        .defaultValue("default")
                        .required(false),
                    bool(INSECURE_SKIP_TLS_VERIFY)
                        .label("Skip TLS Verification")
                        .description(
                            "Trusts any API server certificate. Use it only to reach a test cluster.")
                        .defaultValue(false)
                        .required(false)));

    private KubernetesConnection() {
    }
}
