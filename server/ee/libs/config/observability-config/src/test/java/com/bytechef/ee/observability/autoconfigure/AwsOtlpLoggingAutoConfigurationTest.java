/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.observability.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpHttpLogRecordExporterBuilderCustomizer;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * @version ee
 *
 * @author Igor Beslic
 */
class AwsOtlpLoggingAutoConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(AwsOtlpLoggingAutoConfiguration.class, OtlpLoggingAutoConfiguration.class))
        .withUserConfiguration(TestConfiguration.class)
        .withPropertyValues(
            "management.opentelemetry.logging.export.otlp.endpoint=https://logs.eu-central-1.amazonaws.com/v1/logs");

    @Test
    void testSpringBootExporterIsCustomizedWithAwsSenderAndLogHeaders() {
        applicationContextRunner.withPropertyValues(
            "bytechef.observability.logging.aws.enabled=true", "bytechef.observability.logging.aws.log-group=bytechef",
            "bytechef.observability.logging.aws.log-stream=server-app")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
                    .hasBean("otlpHttpLogRecordExporter")
                    .hasSingleBean(OtlpHttpLogRecordExporterBuilderCustomizer.class);

                // the exporter prints its component loader and header names, the header values are obfuscated
                assertThat(context.getBean(OtlpHttpLogRecordExporter.class)
                    .toString()).contains("AwsHttpSender", "x-aws-log-group=", "x-aws-log-stream=");
            });
    }

    @Test
    void testStartupFailsWhenLogGroupIsMissing() {
        applicationContextRunner.withPropertyValues(
            "bytechef.observability.logging.aws.enabled=true",
            "bytechef.observability.logging.aws.log-stream=server-app")
            .run(context -> assertThat(context).getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytechef.observability.logging.aws.log-group"));
    }

    @Test
    void testStartupFailsWhenLogStreamIsMissing() {
        applicationContextRunner.withPropertyValues(
            "bytechef.observability.logging.aws.enabled=true", "bytechef.observability.logging.aws.log-group=bytechef")
            .run(context -> assertThat(context).getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytechef.observability.logging.aws.log-stream"));
    }

    @Test
    void testLogHeadersConfiguredAsOtlpHeadersAreAccepted() {
        applicationContextRunner.withPropertyValues(
            "bytechef.observability.logging.aws.enabled=true",
            "management.opentelemetry.logging.export.otlp.headers.X-Aws-Log-Group=bytechef",
            "management.opentelemetry.logging.export.otlp.headers.X-Aws-Log-Stream=server-app")
            .run(context -> assertThat(context.getBean(OtlpHttpLogRecordExporter.class)
                .toString()).contains("AwsHttpSender", "X-Aws-Log-Group=", "X-Aws-Log-Stream=")
                    .doesNotContain("x-aws-log-group=", "x-aws-log-stream="));
    }

    @Test
    void testResolveLogHeader() {
        assertThat(AwsOtlpLoggingAutoConfiguration.resolveLogHeader(
            " from-property ", Map.of(), AwsOtlpLoggingAutoConfiguration.LOG_GROUP_HEADER, "log-group"))
                .contains("from-property");

        Map<String, String> otlpHeaders = Map.of("X-AWS-LOG-GROUP", "from-header");

        // already added by Spring Boot, adding it again would send the header twice
        assertThat(AwsOtlpLoggingAutoConfiguration.resolveLogHeader(
            null, otlpHeaders, AwsOtlpLoggingAutoConfiguration.LOG_GROUP_HEADER, "log-group")).isEmpty();
        assertThat(AwsOtlpLoggingAutoConfiguration.resolveLogHeader(
            "from-header", otlpHeaders, AwsOtlpLoggingAutoConfiguration.LOG_GROUP_HEADER, "log-group")).isEmpty();
        assertThatThrownBy(() -> AwsOtlpLoggingAutoConfiguration.resolveLogHeader(
            "from-property", otlpHeaders, AwsOtlpLoggingAutoConfiguration.LOG_GROUP_HEADER, "log-group"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("set only one of them");
    }

    @Test
    void testSpringBootExporterIsNotCustomizedWhenAwsIsDisabled() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
                .doesNotHaveBean(OtlpHttpLogRecordExporterBuilderCustomizer.class);

            assertThat(context.getBean(OtlpHttpLogRecordExporter.class)
                .toString()).doesNotContain("AwsHttpSender", "x-aws-log-group");
        });
    }

    @Test
    void testNoExporterWhenLoggingExportIsDisabled() {
        applicationContextRunner.withPropertyValues(
            "bytechef.observability.logging.aws.enabled=true", "management.logging.export.enabled=false")
            .run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(OtlpHttpLogRecordExporter.class)
                .doesNotHaveBean(OtlpHttpLogRecordExporterBuilderCustomizer.class));
    }

    @Test
    void testResolveRegion() {
        assertThat(AwsOtlpLoggingAutoConfiguration.resolveRegion(
            null, "https://logs.eu-central-1.amazonaws.com/v1/logs")).isEqualTo("eu-central-1");
        assertThat(AwsOtlpLoggingAutoConfiguration.resolveRegion(
            null, "https://xray.us-gov-west-1.amazonaws.com/v1/traces")).isEqualTo("us-gov-west-1");
        assertThat(AwsOtlpLoggingAutoConfiguration.resolveRegion(
            "us-east-1", "https://collector.example.com/v1/logs")).isEqualTo("us-east-1");
        assertThatThrownBy(() -> AwsOtlpLoggingAutoConfiguration.resolveRegion(
            null, "https://collector.example.com/v1/logs")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytechef.observability.logging.aws.region");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestConfiguration {

        @Bean
        AwsCredentialsProvider awsCredentialsProvider() {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDEXAMPLE", "secretExample"));
        }
    }
}
