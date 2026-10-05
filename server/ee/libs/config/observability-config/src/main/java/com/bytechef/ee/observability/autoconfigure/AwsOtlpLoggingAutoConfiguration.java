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
import io.opentelemetry.common.ComponentLoader;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.opentelemetry.autoconfigure.logging.ConditionalOnEnabledLoggingExport;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpHttpLogRecordExporterBuilderCustomizer;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Makes Spring Boot's {@code OtlpHttpLogRecordExporter} sign its requests with AWS Signature Version 4, so logs can be
 * sent straight to the CloudWatch OTLP endpoint without a collector or signing proxy.
 *
 * <p>
 * The exporter stays the one Spring Boot builds from the {@code management.opentelemetry.logging.export.otlp.*}
 * properties; the {@link OtlpHttpLogRecordExporterBuilderCustomizer} only plugs the {@link AwsHttpSender} in through
 * the builder's component loader and adds the CloudWatch headers.
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
@AutoConfiguration(after = OtlpLoggingAutoConfiguration.class)
@ConditionalOnBooleanProperty("bytechef.observability.logging.aws.enabled")
@ConditionalOnClass({
    AwsCredentialsProvider.class, OtlpHttpLogRecordExporterBuilderCustomizer.class
})
@ConditionalOnEnabledLoggingExport("otlp")
@ConditionalOnProperty(
    name = "management.opentelemetry.logging.export.otlp.transport", havingValue = "http", matchIfMissing = true)
public class AwsOtlpLoggingAutoConfiguration {

    static final String LOG_GROUP_HEADER = "x-aws-log-group";
    static final String LOG_STREAM_HEADER = "x-aws-log-stream";

    @Bean
    @ConditionalOnBean(OtlpLoggingConnectionDetails.class)
    OtlpHttpLogRecordExporterBuilderCustomizer awsOtlpHttpLogRecordExporterBuilderCustomizer(
        ApplicationProperties applicationProperties, OtlpLoggingProperties otlpLoggingProperties,
        OtlpLoggingConnectionDetails otlpLoggingConnectionDetails,
        ObjectProvider<AwsCredentialsProvider> awsCredentialsProviderObjectProvider) {

        ApplicationProperties.Observability observability = applicationProperties.getObservability();

        ApplicationProperties.Observability.Logging.Aws aws = observability.getLogging()
            .getAws();

        AwsSigV4Signer signer = new AwsSigV4Signer(
            resolveRegion(aws.getRegion(), otlpLoggingConnectionDetails.getUrl(Transport.HTTP)), aws.getService());

        AwsCredentialsProvider awsCredentialsProvider = awsCredentialsProviderObjectProvider.getIfUnique(
            () -> DefaultCredentialsProvider.builder()
                .build());

        Supplier<AwsSigV4Signer.Credentials> credentialsSupplier = () -> toCredentials(
            awsCredentialsProvider.resolveCredentials());

        ComponentLoader componentLoader = AwsHttpSender.componentLoader(signer, credentialsSupplier);

        // Resolved here, not in the customizer, so a missing value fails the startup
        Map<String, String> otlpHeaders = otlpLoggingProperties.getHeaders();

        Optional<String> logGroup = resolveLogHeader(aws.getLogGroup(), otlpHeaders, LOG_GROUP_HEADER, "log-group");
        Optional<String> logStream = resolveLogHeader(
            aws.getLogStream(), otlpHeaders, LOG_STREAM_HEADER, "log-stream");

        return builder -> {
            builder.setComponentLoader(componentLoader);

            logGroup.ifPresent(value -> builder.addHeader(LOG_GROUP_HEADER, value));
            logStream.ifPresent(value -> builder.addHeader(LOG_STREAM_HEADER, value));
        };
    }

    /**
     * Returns the header value to add to the exporter, or empty when the header is already one of the
     * {@code management.opentelemetry.logging.export.otlp.headers}, which Spring Boot adds itself.
     *
     * <p>
     * CloudWatch rejects every request without the log group and log stream headers with HTTP 400, so a missing value
     * fails the startup instead of every export. A value configured in both places must be the same: sending the header
     * twice would break the request signature.
     */
    static Optional<String> resolveLogHeader(
        String propertyValue, Map<String, String> otlpHeaders, String headerName, String propertyName) {

        Map<String, String> caseInsensitiveOtlpHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        caseInsensitiveOtlpHeaders.putAll(otlpHeaders);

        String otlpHeaderValue = caseInsensitiveOtlpHeaders.get(headerName);

        String property = "'bytechef.observability.logging.aws." + propertyName + "'";

        if (StringUtils.hasText(otlpHeaderValue)) {
            Assert.state(
                !StringUtils.hasText(propertyValue) || otlpHeaderValue.trim()
                    .equals(propertyValue.trim()),
                property + " and the 'management.opentelemetry.logging.export.otlp.headers." + headerName +
                    "' header have different values, set only one of them");

            return Optional.empty();
        }

        Assert.state(
            StringUtils.hasText(propertyValue),
            property + " must be set when 'bytechef.observability.logging.aws.enabled' is true, CloudWatch requires " +
                "the " + headerName + " request header");

        return Optional.of(propertyValue.trim());
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
