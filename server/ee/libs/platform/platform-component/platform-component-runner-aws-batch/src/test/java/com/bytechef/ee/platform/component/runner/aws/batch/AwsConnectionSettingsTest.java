/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.component.aws.constant.AwsConstants;
import com.bytechef.platform.component.ComponentConnection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AwsConnectionSettingsTest {

    @Test
    void testEveryFieldIsRead() {
        AwsConnectionSettings settings = settings(
            Map.of(
                "accessKeyId", " AKIAEXAMPLE ", "secretAccessKey", "secret-key", "region", "eu-west-1", "roleArn",
                "arn:aws:iam::123456789012:role/bytechef", "externalId", "ext"));

        assertThat(settings.accessKeyId()).isEqualTo("AKIAEXAMPLE");
        assertThat(settings.secretAccessKey()).isEqualTo("secret-key");
        assertThat(settings.awsRegion()).isEqualTo(Region.EU_WEST_1);
        assertThat(settings.roleArn()).isEqualTo("arn:aws:iam::123456789012:role/bytechef");
        assertThat(settings.externalId()).isEqualTo("ext");
    }

    @Test
    void testValidateRequiresKeysAndARegion() {
        assertThatThrownBy(() -> settings(Map.of("secretAccessKey", "s", "region", "eu-west-1")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("access key");

        assertThatThrownBy(() -> settings(Map.of("accessKeyId", "a", "region", "eu-west-1")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("secret access key");

        assertThatThrownBy(() -> settings(Map.of("accessKeyId", "a", "secretAccessKey", "s")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("region");

        settings(Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1")).validate();
    }

    @Test
    void testValidateRejectsARoleArnThatIsNotAnIamRole() {
        assertThatThrownBy(
            () -> settings(
                Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1", "roleArn", "bytechef"))
                    .validate())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("arn:aws");
    }

    @Test
    void testValidateRejectsABudgetTheAssumedRoleCannotCover() {
        AwsConnectionSettings withRole = settings(
            Map.of(
                "accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1", "roleArn",
                "arn:aws:iam::123456789012:role/bytechef"));

        withRole.validateBudget(Duration.ofHours(11)
            .plusMinutes(50));

        assertThatThrownBy(() -> withRole.validateBudget(Duration.ofHours(11)
            .plusMinutes(51)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 hours");

        settings(Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1"))
            .validateBudget(Duration.ofHours(48));
    }

    @Test
    void testStaticKeysWithoutARoleAreUsedAsTheyAre() {
        AwsCredentialsProvider credentialsProvider = settings(
            Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1"))
                .credentialsProvider(Duration.ofHours(1));

        assertThat(credentialsProvider).isInstanceOf(StaticCredentialsProvider.class);

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("a");
        assertThat(credentials.secretAccessKey()).isEqualTo("s");
    }

    @Test
    void testARoleIsAssumedThroughSts() {
        AwsCredentialsProvider credentialsProvider = settings(
            Map.of(
                "accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1", "roleArn",
                "arn:aws:iam::123456789012:role/bytechef"))
                    .credentialsProvider(Duration.ofHours(1));

        assertThat(credentialsProvider).isInstanceOf(SdkAutoCloseable.class);

        ((SdkAutoCloseable) credentialsProvider).close();
    }

    @Test
    void testClosingAnAssumedRoleReleasesTheStsClientAndTheSessionCache() {
        StsClient stsClient = mock(StsClient.class);
        StsAssumeRoleCredentialsProvider delegate = mock(StsAssumeRoleCredentialsProvider.class);
        AwsCredentials credentials = AwsBasicCredentials.create("assumed-a", "assumed-s");

        when(delegate.resolveCredentials()).thenReturn(credentials);

        AssumedRoleCredentialsProvider provider = new AssumedRoleCredentialsProvider(stsClient, delegate);

        assertThat(provider.resolveCredentials()).isEqualTo(credentials);

        provider.close();

        verify(delegate).close();
        verify(stsClient).close();
    }

    @Test
    void testTheUrlValidityIsTheBudgetPlusTenMinutes() {
        assertThat(AwsConnectionSettings.urlValidity(Duration.ofHours(1))).isEqualTo(Duration.ofMinutes(70));
    }

    @Test
    void testToStringNeverShowsTheSecretKeyOrTheFullAccessKeyId() {
        String text = settings(
            Map.of("accessKeyId", "AKIAIOSFODNN7EXAMPLE", "secretAccessKey", "secret-key", "region", "eu-west-1"))
                .toString();

        assertThat(text).doesNotContain("secret-key")
            .doesNotContain("AKIAIOSFODNN7EXAMPLE")
            .contains("accessKeyId=****MPLE")
            .contains("eu-west-1");
    }

    @Test
    void testToStringFullyMasksAShortAccessKeyId() {
        String text = settings(Map.of("accessKeyId", "AKIA", "secretAccessKey", "secret-key", "region", "eu-west-1"))
            .toString();

        assertThat(text).doesNotContain("AKIA")
            .contains("accessKeyId=****,");
    }

    @Test
    void testTheConnectionComponentDeclaresEveryParsedField() {
        assertThat(AwsConnectionSettings.ACCESS_KEY_ID).isEqualTo(AwsConstants.ACCESS_KEY_ID);
        assertThat(AwsConnectionSettings.SECRET_ACCESS_KEY).isEqualTo(AwsConstants.SECRET_ACCESS_KEY);
        assertThat(AwsConnectionSettings.REGION).isEqualTo(AwsConstants.REGION);
        assertThat(AwsConnectionSettings.ROLE_ARN).isEqualTo(AwsConstants.ROLE_ARN);
        assertThat(AwsConnectionSettings.EXTERNAL_ID).isEqualTo(AwsConstants.EXTERNAL_ID);
    }

    private static AwsConnectionSettings settings(Map<String, ?> parameters) {
        return AwsConnectionSettings.of(new ComponentConnection("aws", 1, 1L, new HashMap<>(parameters), null));
    }
}
