/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.observability.autoconfigure;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.observability.aws.AwsHttpSender;
import com.bytechef.ee.observability.aws.AwsSigV4Signer;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.ConditionalOnEnabledLoggingExport;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Replaces Spring Boot's {@link OtlpHttpLogRecordExporter} with one whose requests are signed with AWS Signature
 * Version 4, so logs can be sent straight to the CloudWatch OTLP endpoint without a collector or signing proxy.
 *
 * <p>
 * The exporter is built exactly like Spring Boot builds it, from {@code management.opentelemetry.logging.export.otlp.*}
 * properties; the only difference is the {@link AwsHttpSender} plugged in through the builder's component loader.
 * Running before {@link OtlpLoggingAutoConfiguration} makes its {@code @ConditionalOnMissingBean} exporter back off.
 *
 * <p>
 * Credentials come from the application's {@link AwsCredentialsProvider} bean when there is exactly one, otherwise from
 * the AWS SDK default chain (environment variables, system properties, web identity token, ECS container and EC2
 * instance profile credentials). They are resolved on every request, so rotated credentials are picked up.
 *
 * <p>
 * CloudWatch requires the target log group and log stream, which must already exist, as request headers; they come from
 * {@code bytechef.observability.logging.aws.log-group} and {@code log-stream}.
 *
 * @version ee
 *
 * @author Igor Beslic
 */
@AutoConfiguration(before = OtlpLoggingAutoConfiguration.class)
@ConditionalOnBooleanProperty("bytechef.observability.logging.aws.enabled")
@ConditionalOnClass({
    AwsCredentialsProvider.class, OtlpHttpLogRecordExporter.class
})
@ConditionalOnEnabledLoggingExport("otlp")
@ConditionalOnProperty(
    name = "management.opentelemetry.logging.export.otlp.transport", havingValue = "http", matchIfMissing = true)
@EnableConfigurationProperties(OtlpLoggingProperties.class)
public class AwsOtlpLoggingAutoConfiguration {

    static final String LOG_GROUP_HEADER = "x-aws-log-group";
    static final String LOG_STREAM_HEADER = "x-aws-log-stream";

    @Bean
    OtlpHttpLogRecordExporter awsOtlpHttpLogRecordExporter(
        ApplicationProperties applicationProperties, OtlpLoggingProperties otlpLoggingProperties,
        ObjectProvider<AwsCredentialsProvider> awsCredentialsProviderObjectProvider,
        ObjectProvider<MeterProvider> meterProviderObjectProvider) {

        String endpoint = otlpLoggingProperties.getEndpoint();

        Assert.state(
            StringUtils.hasText(endpoint), "'management.opentelemetry.logging.export.otlp.endpoint' must be set");

        ApplicationProperties.Observability observability = applicationProperties.getObservability();

        ApplicationProperties.Observability.Logging.Aws aws = observability.getLogging()
            .getAws();

        AwsSigV4Signer signer = new AwsSigV4Signer(resolveRegion(aws.getRegion(), endpoint), aws.getService());

        AwsCredentialsProvider awsCredentialsProvider = awsCredentialsProviderObjectProvider.getIfUnique(
            () -> DefaultCredentialsProvider.builder()
                .build());

        Supplier<AwsSigV4Signer.Credentials> credentialsSupplier = () -> toCredentials(
            awsCredentialsProvider.resolveCredentials());

        OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
            .setEndpoint(endpoint)
            .setTimeout(otlpLoggingProperties.getTimeout())
            .setConnectTimeout(otlpLoggingProperties.getConnectTimeout())
            .setCompression(otlpLoggingProperties.getCompression()
                .name()
                .toLowerCase(Locale.US))
            .setComponentLoader(AwsHttpSender.componentLoader(signer, credentialsSupplier));

        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        headers.putAll(otlpLoggingProperties.getHeaders());

        headers.put(LOG_GROUP_HEADER, resolveLogHeader(aws.getLogGroup(), headers, LOG_GROUP_HEADER, "log-group"));
        headers.put(
            LOG_STREAM_HEADER, resolveLogHeader(aws.getLogStream(), headers, LOG_STREAM_HEADER, "log-stream"));

        headers.forEach(builder::addHeader);

        meterProviderObjectProvider.ifAvailable(builder::setMeterProvider);

        return builder.build();
    }

    /**
     * CloudWatch rejects every request without the log group and log stream headers with HTTP 400, so a missing value
     * fails the startup instead of every export.
     */
    static String resolveLogHeader(
        String propertyValue, Map<String, String> headers, String headerName, String propertyName) {

        String value = StringUtils.hasText(propertyValue) ? propertyValue : headers.get(headerName);

        Assert.state(
            StringUtils.hasText(value),
            "'bytechef.observability.logging.aws." + propertyName + "' must be set when " +
                "'bytechef.observability.logging.aws.enabled' is true, CloudWatch requires the " + headerName +
                " request header");

        return value.trim();
    }

    static String resolveRegion(String region, String endpoint) {
        if (StringUtils.hasText(region)) {
            return region;
        }

        String host = URI.create(endpoint)
            .getHost();

        // {service}.{region}.amazonaws.com or {service}.{region}.amazonaws.com.cn
        String[] labels = host == null ? new String[0] : host.split("\\.");

        boolean amazonAwsHost = labels.length >= 4 && labels[2].equals("amazonaws") && labels[3].equals("com");

        Assert.state(
            amazonAwsHost,
            "'bytechef.observability.logging.aws.region' must be set, it cannot be derived from the endpoint " +
                endpoint);

        return labels[1];
    }

    private static AwsSigV4Signer.Credentials toCredentials(AwsCredentials awsCredentials) {
        String sessionToken = awsCredentials instanceof AwsSessionCredentials awsSessionCredentials
            ? awsSessionCredentials.sessionToken() : null;

        return new AwsSigV4Signer.Credentials(
            awsCredentials.accessKeyId(), awsCredentials.secretAccessKey(), sessionToken);
    }
}
