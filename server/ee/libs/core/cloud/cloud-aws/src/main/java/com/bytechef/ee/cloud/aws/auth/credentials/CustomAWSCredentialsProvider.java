/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cloud.aws.auth.credentials;

import com.bytechef.config.ApplicationProperties.Cloud.Aws;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomAWSCredentialsProvider implements AwsCredentialsProvider {

    private final Aws aws;

    @SuppressFBWarnings("EI")
    public CustomAWSCredentialsProvider(Aws aws) {
        this.aws = aws;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return AwsBasicCredentials.create(aws.getAccessKeyId(), aws.getSecretAccessKey());
    }
}
