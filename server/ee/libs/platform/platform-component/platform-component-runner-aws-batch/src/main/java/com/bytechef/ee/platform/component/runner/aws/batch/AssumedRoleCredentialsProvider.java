/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Assumed-role credentials that release both the STS session cache and the STS client they were built on.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class AssumedRoleCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {

    private final StsAssumeRoleCredentialsProvider delegate;
    private final StsClient stsClient;

    AssumedRoleCredentialsProvider(StsClient stsClient, StsAssumeRoleCredentialsProvider delegate) {
        this.stsClient = stsClient;
        this.delegate = delegate;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return delegate.resolveCredentials();
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } finally {
            stsClient.close();
        }
    }
}
