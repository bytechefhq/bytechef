/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.impl.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.file.storage.aws.api.AwsFileStorageService;
import com.bytechef.ee.file.storage.aws.impl.service.AwsFileStorageServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "file-storage.provider", havingValue = "aws")
@ComponentScan("io.awspring.cloud")
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableAutoConfiguration
public class AwsFileStorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsFileStorageConfiguration.class);

    public AwsFileStorageConfiguration(ApplicationProperties applicationProperties) {
        if (log.isInfoEnabled()) {
            String bucket = applicationProperties.getFileStorage()
                .getAws()
                .getBucket();

            log.info("File storage provider type enabled: aws, bucket %s".formatted(bucket));
        }
    }

    @Bean
    AwsFileStorageService awsFileStorageService(S3Template s3Template, ApplicationProperties applicationProperties) {
        return new AwsFileStorageServiceImpl(s3Template, applicationProperties.getFileStorage()
            .getAws()
            .getBucket());
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    S3Template s3Template(
        S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider,
        S3ObjectConverter s3ObjectConverter, S3Presigner s3Presigner) {
        return new S3Template(s3Client, s3OutputStreamProvider, s3ObjectConverter, s3Presigner);
    }

    @Bean
    AwsCredentialsProvider awsCredentialsProvider() {
        return new AwsCredentialsProvider() {

            @Override
            public AwsCredentials resolveCredentials() {
                return AwsBasicCredentials.create("noop", "noop");
            }
        };
    }

    @Bean
    AwsRegionProvider awsRegionProvider() {
        return new AwsRegionProvider() {
            @Override
            public Region getRegion() {
                return Region.US_EAST_1;
            }
        };
    }
}
