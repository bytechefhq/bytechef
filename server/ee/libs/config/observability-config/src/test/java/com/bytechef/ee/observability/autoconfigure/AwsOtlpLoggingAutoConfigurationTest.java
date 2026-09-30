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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
    void testAwsExporterReplacesSpringBootExporter() {
        applicationContextRunner.withPropertyValues("bytechef.observability.logging.aws.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
                    .hasBean("awsOtlpHttpLogRecordExporter");
            });
    }

    @Test
    void testSpringBootExporterIsUsedWhenAwsIsDisabled() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
                .doesNotHaveBean("awsOtlpHttpLogRecordExporter");
        });
    }

    @Test
    void testNoExporterWhenLoggingExportIsDisabled() {
        applicationContextRunner.withPropertyValues(
            "bytechef.observability.logging.aws.enabled=true", "management.logging.export.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class));
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
