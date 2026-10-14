/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Label and annotation names the Kubernetes runner puts on what it creates, and the label value a job key maps to.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class KubernetesJobLabels {

    static final String ANNOTATION_KEY = "bytechef.io/key";
    static final String ANNOTATION_KILL_GRACE_PERIOD = "bytechef.io/kill-grace-period";
    static final String ANNOTATION_PULL_SECRET = "bytechef.io/pull-secret";
    static final String LABEL_JOB_ID = "bytechef.io/job-id";
    static final String LABEL_MANAGED_BY = "bytechef.io/managed-by";
    static final String LABEL_TASK_EXECUTION_ID = "bytechef.io/task-execution-id";
    static final String MANAGED_BY = "bytechef";
    static final String RESERVED_PREFIX = "bytechef.io/";

    private static final Pattern LABEL_VALUE = Pattern.compile("^[A-Za-z0-9]([-A-Za-z0-9_.]{0,61}[A-Za-z0-9])?$");

    private KubernetesJobLabels() {
    }

    static String labelValue(String key) {
        if (LABEL_VALUE.matcher(key)
            .matches()) {

            return key;
        }

        return "k-" + sha256Hex(key).substring(0, 16);
    }

    static Map<String, String> labels(RemoteJobKey key) {
        Map<String, String> labels = selector(key);

        Long jobId = key.jobId();

        if (jobId != null) {
            labels.put(LABEL_JOB_ID, String.valueOf(jobId));
        }

        return labels;
    }

    static Map<String, String> selector(RemoteJobKey key) {
        Map<String, String> selector = new LinkedHashMap<>();

        selector.put(LABEL_MANAGED_BY, MANAGED_BY);
        selector.put(LABEL_TASK_EXECUTION_ID, labelValue(key.value()));

        return selector;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                .formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
