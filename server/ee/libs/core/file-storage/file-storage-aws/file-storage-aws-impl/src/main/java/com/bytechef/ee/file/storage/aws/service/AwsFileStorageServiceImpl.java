/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.file.storage.aws.AwsFileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

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
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public void deleteFile(String directory, FileEntry fileEntry) {
        if (fileEntry != null) {
            findObject(directory, fileEntry.getName())
                .ifPresent(s3Resource -> s3Template.deleteObject(
                    bucketName, Objects.requireNonNull(s3Resource.getFilename())));
        }
    }

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException {
        return s3Template.objectExists(bucketName, resolveKey(fileEntry));
    }

    @Override
    public boolean fileExists(String directory, String filename) throws FileStorageException {
        String key = combinePaths(directory, filename);

        return s3Template.listObjects(bucketName, key)
            .stream()
            .map(S3Resource::getFilename)
            .anyMatch(key::equals);
    }

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public long getContentLength(String directory, FileEntry fileEntry) throws FileStorageException {
        S3Resource s3Resource = resolveResource(fileEntry);

        return s3Resource.contentLength();
    }

    @Override
    public FileEntry getFileEntry(String directory, String filename) {
        S3Resource s3Resource = getObject(directory, filename);

        return new FileEntry(filename, URL_PREFIX + bucketName + "/" + s3Resource.getFilename());
    }

    @Override
    public Set<FileEntry> getFileEntries(String directory) throws FileStorageException {
        String prefix = combinePaths(directory, null);

        return s3Template.listObjects(bucketName, prefix)
            .stream()
            .map(S3Resource::getFilename)
            .filter(Objects::nonNull)
            .map(filename -> new FileEntry(
                filename.substring(filename.lastIndexOf('/')), URL_PREFIX + bucketName + "/" + filename))
            .collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public URL getFileEntryURL(String directory, FileEntry fileEntry) {
        S3Resource s3Resource = resolveResource(fileEntry);

        try {
            return s3Resource.getURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String directory, FileEntry fileEntry) {
        return new ByteArrayInputStream(readFileToBytes(directory, fileEntry));
    }

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public OutputStream getOutputStream(String directory, FileEntry fileEntry) throws FileStorageException {
        S3Resource s3Resource = resolveResource(fileEntry);

        try {
            return s3Resource.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException {
        S3Resource s3Resource = resolveResource(fileEntry);

        byte[] bytes;

        try (InputStream inputStream = s3Resource.getInputStream()) {
            bytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return MIME_DECODER.decode(bytes);
    }

    @Override
    public String readFileToString(String directory, FileEntry fileEntry) throws FileStorageException {
        byte[] bytes = readFileToBytes(directory, fileEntry);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String getType() {
        return ApplicationProperties.FileStorage.Provider.AWS.name();
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data) throws FileStorageException {
        return doStoreFileContent(directory, filename, data, false);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data, boolean generateFilename)
        throws FileStorageException {

        return doStoreFileContent(directory, filename, data, generateFilename);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data) {
        return storeFileContent(directory, filename, data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data, boolean generateFilename)
        throws FileStorageException {

        return storeFileContent(directory, filename, data.getBytes(StandardCharsets.UTF_8), generateFilename);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, InputStream inputStream)
        throws FileStorageException {

        try {
            return storeFileContent(directory, filename, inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileEntry storeFileContent(
        String directory, String filename, InputStream inputStream, boolean generateFilename)
        throws FileStorageException {

        try {
            return storeFileContent(directory, filename, inputStream.readAllBytes(), generateFilename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileEntry doStoreFileContent(String directory, String filename, byte[] data, boolean generateFilename) {
        String storageFilename = generateFilename ? generateUniqueFilename(filename) : filename;

        String path = combinePaths(directory, storageFilename);

        s3Template.store(bucketName, path, data);

        return new FileEntry(filename, URL_PREFIX + bucketName + "/" + path);
    }

    private S3Resource getObject(String directoryPath, String filename) {
        return findObject(directoryPath, filename)
            .orElseThrow(() -> new FileStorageException("File %s doesn't exist".formatted(filename)));
    }

    private S3Resource resolveResource(FileEntry fileEntry) {
        String key = resolveKey(fileEntry);

        if (!s3Template.objectExists(bucketName, key)) {
            throw new FileStorageException("File %s doesn't exist".formatted(fileEntry.getName()));
        }

        return s3Template.download(bucketName, key);
    }

    private String resolveKey(FileEntry fileEntry) {
        String prefix = URL_PREFIX + bucketName + "/";
        String url = fileEntry.getUrl();

        if (!url.startsWith(prefix)) {
            throw new FileStorageException("Invalid file URL: " + url);
        }

        return url.substring(prefix.length());
    }

    private static String generateUniqueFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        UUID uuid = UUID.randomUUID();

        return dotIndex > 0 ? uuid + filename.substring(dotIndex) : uuid.toString();
    }

    private Optional<S3Resource> findObject(String directoryPath, String filename) {
        String key = combinePaths(directoryPath, filename);

        return s3Template.listObjects(bucketName, key)
            .stream()
            .filter(s3Resource -> key.equals(s3Resource.getFilename()))
            .findFirst();
    }

    private static String combinePaths(String directory, String filename) {
        directory = StringUtils.replace(directory.replaceAll("[^0-9a-zA-Z/_!\\-.*'()]", ""), " ", "");

        String path = TenantContext.getCurrentTenantId() + "/" + directory;

        if (filename != null) {
            path += "/" + filename;
        }

        return path;
    }
}
