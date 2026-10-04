/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.tenant.TenantContext;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * @version ee
 *
 * @author Marko Krikovic
 */
@SpringBootTest
@Testcontainers
class AwsFileStorageIntTest {

    private static final String BUCKET_NAME = String.valueOf(UUID.randomUUID());
    private static final String DATA = "Hello World";
    private static final String DIR_PATH = "RandomDirectory/Test";
    private static final String TENANT_ID = "public";
    private static final String KEY = "key";
    private static final String FILE_PATH = "s3://" + BUCKET_NAME + "/" + TENANT_ID + "/" + DIR_PATH + "/" + KEY;

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"));

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Template s3Template;

    @Autowired
    private AwsFileStorageServiceImpl storageService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.s3.endpoint", () -> String.valueOf(localStack.getEndpointOverride(S3)));
        registry.add("bytechef.file-storage.aws.bucket", () -> BUCKET_NAME);
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
    }

    @Test
    void canStoreFileContent() {
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(msg.getName()).isEqualTo(KEY);
                assertThat(msg.getUrl()).isEqualTo(FILE_PATH);
            });
    }

    @Test
    void canDeterminateIfFileExists() {
        FileEntry msg = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        boolean existsFileEntry = storageService.fileExists(DIR_PATH, msg);
        boolean existsString = storageService.fileExists(DIR_PATH, KEY);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(existsFileEntry).isTrue();
                assertThat(existsString).isTrue();
            });
    }

    @Test
    void canGetFileEntryUrl() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        URL url = storageService.getFileEntryURL(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(url.toString())
                    .matches(
                        "http://127\\.0\\.0\\.1:\\d+/" + BUCKET_NAME + "/" + TENANT_ID + "/" + DIR_PATH + "/" + KEY);
            });
    }

    @Test
    void canReadFileToBytes() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        byte[] bytes = storageService.readFileToBytes(DIR_PATH, fileEntry);

        String result = new String(bytes, StandardCharsets.UTF_8);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(result).isEqualTo(DATA);
            });
    }

    @Test
    void canReadFileToString() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        String result = storageService.readFileToString(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(result).isEqualTo(DATA);
            });
    }

    @Test
    void canGetFileStream() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        InputStream fileStream = storageService.getInputStream(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                String result = new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);

                assertThat(result).isEqualTo(DATA);
            });
    }

    @Test
    void canGetFileEntry() {
        storageService.storeFileContent(DIR_PATH, KEY, DATA);

        FileEntry result = storageService.getFileEntry(DIR_PATH, KEY);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(result.getName()).isEqualTo(KEY);
                assertThat(result.getUrl()).isEqualTo(FILE_PATH);
            });
    }

    @Test
    void canGetFileEntries() {
        FileEntry fileEntry1 = storageService.storeFileContent(DIR_PATH, KEY, DATA);
        FileEntry fileEntry2 = storageService.storeFileContent(DIR_PATH, "key2", DATA);
        Set<FileEntry> fileEntries = storageService.getFileEntries(DIR_PATH);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(fileEntries).contains(fileEntry1);
                assertThat(fileEntries).contains(fileEntry2);
            });
    }

    @Test
    void getFileEntriesExcludesSiblingDirectoriesSharingANamePrefix() {
        FileEntry fileEntry = storageService.storeFileContent("PrefixDirectory/1", KEY, DATA);

        storageService.storeFileContent("PrefixDirectory/10", KEY, DATA);

        assertThat(storageService.getFileEntries("PrefixDirectory/1")).containsExactly(fileEntry);
    }

    @Test
    void getFileEntriesListsPastOneThousandKeys() {
        int fileCount = 1001;

        for (int index = 0; index < fileCount; index++) {
            storageService.storeFileContent("PagedDirectory", "key" + index, DATA);
        }

        assertThat(storageService.getFileEntries("PagedDirectory")).hasSize(fileCount);
    }

    @Test
    void getFileEntriesIncludesKeysInNestedDirectories() {
        FileEntry topLevelFileEntry = storageService.storeFileContent("NestedDirectory/1", KEY, DATA);
        FileEntry nestedFileEntry = storageService.storeFileContent("NestedDirectory/1/user_7/0", KEY, DATA);

        assertThat(storageService.getFileEntries("NestedDirectory/1"))
            .containsExactlyInAnyOrder(topLevelFileEntry, nestedFileEntry);
    }

    @Test
    void getFileEntriesAcceptsADirectoryWithATrailingSlash() {
        FileEntry fileEntry = storageService.storeFileContent("TrailingSlashDirectory/123", KEY, DATA);

        assertThat(storageService.getFileEntries("TrailingSlashDirectory/123/")).containsExactly(fileEntry);
    }

    @Test
    void getFileEntriesSkipsFolderMarkerKeys() {
        FileEntry fileEntry = storageService.storeFileContent("MarkerDirectory/1", KEY, DATA);

        putRawObject("MarkerDirectory/1/", DATA);
        putRawObject("MarkerDirectory/1/nested/", DATA);

        assertThat(storageService.getFileEntries("MarkerDirectory/1")).containsExactly(fileEntry);
    }

    @Test
    void readingAnObjectThatIsNotBase64ThrowsFileStorageException() {
        putRawObject("RawDirectory/" + KEY, "a=b");

        FileEntry fileEntry = storageService.getFileEntry("RawDirectory", KEY);

        assertThatThrownBy(() -> storageService.readFileToBytes("RawDirectory", fileEntry))
            .isInstanceOf(FileStorageException.class);
    }

    @Test
    void readingAFileDeletedAfterTheExistenceCheckThrowsFileStorageException() {
        S3Template existenceCheckPassingS3Template = Mockito.spy(s3Template);

        Mockito.doReturn(true)
            .when(existenceCheckPassingS3Template)
            .objectExists(Mockito.anyString(), Mockito.anyString());

        AwsFileStorageServiceImpl racingStorageService = new AwsFileStorageServiceImpl(
            s3Client, existenceCheckPassingS3Template, BUCKET_NAME);

        FileEntry fileEntry = storageService.storeFileContent("VanishingDirectory", KEY, DATA);

        storageService.deleteFile("VanishingDirectory", fileEntry);

        assertThatThrownBy(() -> racingStorageService.readFileToString("VanishingDirectory", fileEntry))
            .isInstanceOf(FileStorageException.class);
    }

    @Test
    void deleteFileRemovesAnEntryListedFromAParentDirectory() {
        storageService.storeFileContent("NestedDelete/child", KEY, DATA);

        FileEntry fileEntry = storageService.getFileEntries("NestedDelete")
            .iterator()
            .next();

        storageService.deleteFile("NestedDelete", fileEntry);

        assertThat(storageService.getFileEntries("NestedDelete")).isEmpty();
    }

    @Test
    void canDeleteFile() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        storageService.deleteFile(DIR_PATH, fileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                boolean exists = storageService.fileExists(DIR_PATH, KEY);

                assertThat(exists).isFalse();
            });
    }

    @Test
    void deleteFileIsIdempotentWhenObjectMissing() {
        FileEntry fileEntry = new FileEntry(
            "missing-key", "s3://" + BUCKET_NAME + "/" + TENANT_ID + "/" + DIR_PATH + "/missing-key");

        // Deleting an object that is not present must be a no-op, not a FileStorageException. Regression for a
        // knowledge base document whose chunk content file was already gone from the bucket (issue #5392).
        assertThatCode(() -> storageService.deleteFile(DIR_PATH, fileEntry)).doesNotThrowAnyException();
    }

    @Test
    void deleteFileIsNoOpWhenFileEntryIsNull() {
        // A knowledge base document with no attached file (only chunk content) has a null FileEntry. Regression for
        // a NullPointerException on fileEntry.getName() (issue #5392).
        assertThatCode(() -> storageService.deleteFile(DIR_PATH, null)).doesNotThrowAnyException();
    }

    @Test
    void fileLookupIsTenantScoped() {
        FileEntry fileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> assertThat(storageService.fileExists(DIR_PATH, KEY)).isTrue());

        try {
            TenantContext.setCurrentTenantId("other");

            assertThat(storageService.fileExists(DIR_PATH, KEY)).isFalse();
            assertThat(storageService.getFileEntries(DIR_PATH)).isEmpty();

            assertThatCode(() -> storageService.deleteFile(DIR_PATH, fileEntry)).doesNotThrowAnyException();
        } finally {
            TenantContext.resetCurrentTenantId();
        }

        assertThat(storageService.fileExists(DIR_PATH, KEY)).isTrue();
    }

    @Test
    void deletingPreviousVersionDoesNotDeleteReplacementWithGeneratedFilename() {
        FileEntry oldFileEntry = storageService.storeFileContent(DIR_PATH, KEY, DATA, true);
        FileEntry newFileEntry = storageService.storeFileContent(DIR_PATH, KEY, "Updated content", true);

        storageService.deleteFile(DIR_PATH, oldFileEntry);

        await()
            .pollInterval(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(10))
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertThat(storageService.readFileToString(DIR_PATH, newFileEntry)).isEqualTo("Updated content");
            });
    }

    private void putRawObject(String path, String content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(TENANT_ID + "/" + path)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(content));
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    @ImportAutoConfiguration({
        io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration.class,
        io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration.class,
        io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration.class,
        io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class,
        io.awspring.cloud.autoconfigure.s3.S3TransferManagerAutoConfiguration.class
    })
    static class AwsFileStorageIntTestConfiguration {

        @Bean
        AwsFileStorageServiceImpl awsFileStorageService(
            S3Client s3Client, S3Template s3Template, ApplicationProperties applicationProperties) {

            return new AwsFileStorageServiceImpl(s3Client, s3Template, applicationProperties.getFileStorage()
                .getAws()
                .getBucket());
        }

        @Bean
        AwsCredentialsProvider awsCredentialsProvider() {
            return () -> AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey());
        }

        @Bean
        AwsRegionProvider awsRegionProvider() {
            return () -> Region.US_EAST_1;
        }
    }
}
