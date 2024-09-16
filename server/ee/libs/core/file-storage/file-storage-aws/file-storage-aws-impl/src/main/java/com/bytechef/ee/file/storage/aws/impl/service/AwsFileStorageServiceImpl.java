/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.impl.service;

import com.bytechef.ee.file.storage.aws.api.AwsFileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
//@Service
public class AwsFileStorageServiceImpl implements AwsFileStorageService {

    @SuppressWarnings("PMD.UnusedPrivateField")
    private static final String URL_PREFIX = "s3://";
    private final S3Template s3Template;
    private final String bucketName;

    public AwsFileStorageServiceImpl(S3Template s3Template, String bucketName) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
    }

    public String createBucket() {
        return s3Template.createBucket(bucketName);
    }

    public void deleteBucket() {
        s3Template.deleteBucket(bucketName);
    }

    private boolean bucketExists() {
        return s3Template.bucketExists(bucketName);
    }

    private void deleteObject(String key) {
        s3Template.deleteObject(bucketName, key);
    }

    private boolean objectExists(String key) {
        return s3Template.objectExists(bucketName, key);
    }

    private List<S3Resource> listObjects(String prefix) {
        return s3Template.listObjects(bucketName, prefix);
    }

    private void store(String key, Object object) {
        s3Template.store(bucketName, key, object);
    }

    private <T> T read(String key, Class<T> clazz) {
        return s3Template.read(bucketName, key, clazz);
    }

    public URL createSignedGetURL(String key, Duration duration) {
        return s3Template.createSignedGetURL(bucketName, key, duration);
    }

    public URL createSignedPutURL(String key, Duration duration, ObjectMetadata metadata, String contentType) {
        return s3Template.createSignedPutURL(bucketName, key, duration, metadata, contentType);
    }

    private static String getURLString(S3Resource s3) {
        try {
            return s3.getURL()
                .toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String combinePaths(String directoryPath, String key) {
        directoryPath = StringUtils.replace(directoryPath.replaceAll("[^0-9a-zA-Z/_!\\-.*'()]", ""), " ", "");
        directoryPath += "/" + key;
        return directoryPath;
    }

    @Override
    public void deleteFile(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        if (!fileExists(directoryPath, fileEntry.getName()))
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));

        deleteObject(directoryPath + "/" + fileEntry.getName());
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException {
        return fileExists(directoryPath, fileEntry.getName());
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull String key) throws FileStorageException {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));

        return objectExists(directoryPath + "/" + key);
    }

    @Override
    public FileEntry getFileEntry(@NonNull String directoryPath, @NonNull String key) {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        if (!fileExists(directoryPath, key))
            throw new FileStorageException("File %s doesn't exist".formatted(key));

        directoryPath = combinePaths(directoryPath, key);

        return new FileEntry(key, URL_PREFIX + bucketName + "/" + directoryPath);
    }

    @Override
    public Set<FileEntry> getFileEntries(@NonNull String directoryPath) throws FileStorageException {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));

        List<S3Resource> s3Resources = listObjects(directoryPath);

        return s3Resources.stream()
            .map(s3 -> new FileEntry(
                Objects.requireNonNull(s3.getFilename())
                    .substring(
                        s3.getFilename()
                            .lastIndexOf('/')),
                URL_PREFIX + bucketName + "/" + s3.getFilename()))
            .collect(Collectors.toSet());
    }

    @Override
    public InputStream getFileStream(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        if (!fileExists(directoryPath, fileEntry.getName()))
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));

        return new ByteArrayInputStream(readFileToBytes(directoryPath, fileEntry));
    }

    @Override
    public URL getFileEntryURL(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        if (!fileExists(directoryPath, fileEntry.getName()))
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));

        S3Resource download = s3Template.download(bucketName, directoryPath + "/" + fileEntry.getName());
        try {
            return download.getURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readFileToBytes(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        if (!fileExists(directoryPath, fileEntry.getName()))
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));

        return read(directoryPath + "/" + fileEntry.getName(), byte[].class);
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
        if (!bucketExists())
            throw new FileStorageException("Bucket %s doesn't exist.".formatted(bucketName));
        directoryPath = combinePaths(directoryPath, key);

        store(directoryPath, data);

        return new FileEntry(key, URL_PREFIX + bucketName + "/" + directoryPath);
    }

    @Override
    public FileEntry
        storeFileContent(@NonNull String directoryPath, @NonNull String filename, byte[] data, boolean randomFilename)
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
    public FileEntry
        storeFileContent(@NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream)
            throws FileStorageException {
        try {
            return storeFileContent(directoryPath, filename, inputStream.readAllBytes());
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

}
