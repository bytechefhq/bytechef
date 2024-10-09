/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Aws;
import com.bytechef.ee.file.storage.aws.AwsFileStorageService;
import com.bytechef.ee.file.storage.aws.service.AwsFileStorageServiceImpl;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "file-storage.provider", havingValue = "aws")
class AwsFileStorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsFileStorageConfiguration.class);

    AwsFileStorageConfiguration(ApplicationProperties applicationProperties) {
        if (log.isInfoEnabled()) {
            String bucket = applicationProperties.getFileStorage()
                .getAws()
                .getBucket();

            log.info("File storage provider type enabled: aws, bucket {}", bucket);
        }
    }

    @Bean
    AwsFileStorageService awsFileStorageService(S3Template s3Template, ApplicationProperties applicationProperties) {
        Aws aws = applicationProperties.getFileStorage()
            .getAws();

        return new AwsFileStorageServiceImpl(s3Template, aws.getBucket());
    }

    @Bean
    S3Template s3Template(
        S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider, S3ObjectConverter s3ObjectConverter,
        S3Presigner s3Presigner) {

        return new S3Template(s3Client, s3OutputStreamProvider, s3ObjectConverter, s3Presigner);
    }
}
