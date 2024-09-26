/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.service;

import com.bytechef.ee.file.storage.aws.AwsFileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.platform.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsFileStorageServiceImpl implements AwsFileStorageService {

    private static final Base64.Decoder MIME_DECODER = Base64.getMimeDecoder();
    private static final String URL_PREFIX = "s3://";

    private final S3Template s3Template;
    private final String bucketName;

    @SuppressFBWarnings("EI")
    public AwsFileStorageServiceImpl(S3Template s3Template, String bucketName) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
    }

    @Override
    public void deleteFile(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        if (!bucketExists()) {
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        }

        if (!fileExists(directoryPath, fileEntry.getName())) {
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));
        }

        S3Resource s3Resource = getObject(directoryPath, fileEntry.getName());

        s3Template.deleteObject(bucketName, Objects.requireNonNull(s3Resource.getFilename()));
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException {
        return fileExists(directoryPath, fileEntry.getName());
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull String key) throws FileStorageException {
        if (!bucketExists()) {
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        }

        List<S3Resource> s3Resources = listAllObjects();

        return s3Resources.stream()
            .map(S3Resource::getFilename)
            .filter(Objects::nonNull)
            .anyMatch((filename) -> filename.endsWith(directoryPath + "/" + key));
    }

    @Override
    public FileEntry getFileEntry(@NonNull String directoryPath, @NonNull String key) {
        if (!fileExists(directoryPath, key)) {
            throw new FileStorageException("File %s doesn't exist".formatted(key));
        }

        S3Resource s3Resource = getObject(directoryPath, key);

        return new FileEntry(key, URL_PREFIX + bucketName + "/" + s3Resource.getFilename());
    }

    @Override
    public Set<FileEntry> getFileEntries(@NonNull String directoryPath) throws FileStorageException {
        if (!bucketExists()) {
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        }

        List<S3Resource> s3Resources = listAllObjects();

        return s3Resources.stream()
            .map(S3Resource::getFilename)
            .filter(Objects::nonNull)
            .map(filename -> new FileEntry(
                filename.substring(filename.lastIndexOf('/')), URL_PREFIX + bucketName + "/" + filename))
            .collect(Collectors.toSet());
    }

    @Override
    public InputStream getFileStream(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        if (!fileExists(directoryPath, fileEntry.getName())) {
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));
        }

        return new ByteArrayInputStream(readFileToBytes(directoryPath, fileEntry));
    }

    @Override
    public URL getFileEntryURL(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        if (!fileExists(directoryPath, fileEntry.getName())) {
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));
        }

        S3Resource s3Resource = getObject(directoryPath, fileEntry.getName());

        try {
            return s3Resource.getURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readFileToBytes(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {

        if (!fileExists(directoryPath, fileEntry.getName())) {
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));
        }

        S3Resource s3Resource = getObject(directoryPath, fileEntry.getName());

        byte[] bytes;

        try (InputStream inputStream = s3Resource.getInputStream()) {
            bytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return MIME_DECODER.decode(bytes);
    }

    @Override
    public String readFileToString(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {

        byte[] bytes = readFileToBytes(directoryPath, fileEntry);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public FileEntry storeFileContent(@NonNull String directoryPath, @NonNull String key, byte[] data)
        throws FileStorageException {

        if (!bucketExists()) {
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        }

        directoryPath = combinePaths(directoryPath, key);

        s3Template.store(bucketName, directoryPath, data);

        return new FileEntry(key, URL_PREFIX + bucketName + "/" + directoryPath);
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, byte[] data, boolean randomFilename)
        throws FileStorageException {

        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry storeFileContent(@NonNull String directoryPath, @NonNull String key, @NonNull String data) {
        return storeFileContent(directoryPath, key, data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String key, @NonNull String data, boolean randomFilename)
        throws FileStorageException {

        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String key, @NonNull InputStream inputStream)
        throws FileStorageException {

        try {
            return storeFileContent(directoryPath, key, inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream,
        boolean randomFilename) throws FileStorageException {

        throw new UnsupportedOperationException();
    }

    private boolean bucketExists() {
        return s3Template.bucketExists(bucketName);
    }

    private List<S3Resource> listAllObjects() {
        return s3Template.listObjects(bucketName, "");
    }

    private S3Resource getObject(String directoryPath, String key) {
        List<S3Resource> s3Resources = s3Template.listObjects(bucketName, "");

        return s3Resources.stream()
            .filter((file) -> StringUtils.contains(file.getFilename(), directoryPath + "/" + key))
            .findFirst()
            .orElseThrow(() -> new FileStorageException("File %s doesn't exist".formatted(key)));
    }

    private static String combinePaths(String directoryPath, String key) {
        directoryPath = StringUtils.replace(directoryPath.replaceAll("[^0-9a-zA-Z/_!\\-.*'()]", ""), " ", "");

        directoryPath = TenantContext.getCurrentTenantId() + "/" + directoryPath;

        if (key != null) {
            directoryPath += "/" + key;
        }

        return directoryPath;
    }
}
