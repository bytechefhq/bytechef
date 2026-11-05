/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Job names, tags and S3 locations an AWS Batch run uses.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class AwsBatchJobNames {

    static final String INPUT_OBJECT = "input.tar";
    static final String MANAGED_BY = "bytechef";
    static final String OUTPUT_OBJECT = "output.tar";
    static final String TAG_JOB_ID = "bytechef.io/job-id";
    static final String TAG_KEY = "bytechef.io/key";
    static final String TAG_MANAGED_BY = "bytechef.io/managed-by";
    static final String TAG_PREFIX = "bytechef.io/prefix";

    private static final String JOB_NAME_PREFIX = "bytechef-";
    private static final Pattern NAME_VALUE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,118}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AwsBatchJobNames() {
    }

    static String jobName(RemoteJobKey key) {
        return JOB_NAME_PREFIX + nameValue(key.value());
    }

    static String nameValue(String key) {
        if (NAME_VALUE.matcher(key)
            .matches()) {

            return key;
        }

        return "k-" + sha256Hex(key).substring(0, 16);
    }

    static String newPrefix(RemoteJobKey key) {
        Long jobId = key.jobId();

        byte[] randomBytes = new byte[4];

        SECURE_RANDOM.nextBytes(randomBytes);

        return "bytechef/%s/%s-%s/".formatted(
            jobId == null ? "none" : String.valueOf(jobId), nameValue(key.value()), HexFormat.of()
                .formatHex(randomBytes));
    }

    static Map<String, String> tags(RemoteJobKey key, String prefix) {
        Map<String, String> tags = new LinkedHashMap<>();

        tags.put(TAG_MANAGED_BY, MANAGED_BY);
        tags.put(TAG_KEY, key.value());
        tags.put(TAG_PREFIX, prefix);

        Long jobId = key.jobId();

        if (jobId != null) {
            tags.put(TAG_JOB_ID, String.valueOf(jobId));
        }

        return tags;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                .formatHex(messageDigest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
