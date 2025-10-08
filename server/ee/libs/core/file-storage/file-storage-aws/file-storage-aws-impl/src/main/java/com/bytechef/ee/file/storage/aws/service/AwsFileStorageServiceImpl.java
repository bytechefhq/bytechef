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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    public void deleteFile(String directory, FileEntry fileEntry) {
        S3Resource s3Resource = getObject(directory, fileEntry.getName());

        s3Template.deleteObject(bucketName, Objects.requireNonNull(s3Resource.getFilename()));
    }

    @Override
    public boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException {
        return fileExists(directory, fileEntry.getName());
    }

    @Override
    public boolean fileExists(String directory, String filename) throws FileStorageException {
        List<S3Resource> s3Resources = listAllObjects();

        return s3Resources.stream()
            .map(S3Resource::getFilename)
            .filter(Objects::nonNull)
            .anyMatch((curFilename) -> curFilename.endsWith(directory + "/" + filename));
    }

    @Override
    public long getContentLength(String directory, FileEntry fileEntry) throws FileStorageException {
        S3Resource s3Resource = getObject(directory, fileEntry.getName());

        return s3Resource.contentLength();
    }

    @Override
    public FileEntry getFileEntry(String directory, String filename) {
        S3Resource s3Resource = getObject(directory, filename);

        return new FileEntry(filename, URL_PREFIX + bucketName + "/" + s3Resource.getFilename());
    }

    @Override
    public Set<FileEntry> getFileEntries(String directory) throws FileStorageException {
        List<S3Resource> s3Resources = listAllObjects();

        return s3Resources.stream()
            .map(S3Resource::getFilename)
            .filter(Objects::nonNull)
            .map(filename -> new FileEntry(
                filename.substring(filename.lastIndexOf('/')), URL_PREFIX + bucketName + "/" + filename))
            .collect(Collectors.toSet());
    }

    @Override
    public URL getFileEntryURL(String directory, FileEntry fileEntry) {
        S3Resource s3Resource = getObject(directory, fileEntry.getName());

        try {
            return s3Resource.getURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String directory, FileEntry fileEntry) {
        S3Resource s3Resource = getObject(directory, fileEntry.getName());

        try {
            return s3Resource.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OutputStream getOutputStream(String directory, FileEntry fileEntry) throws FileStorageException {
        S3Resource s3Resource = getObject(directory, fileEntry.getName());

        try {
            return s3Resource.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException {
        S3Resource s3Resource = getObject(directory, fileEntry.getName());

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
        directory = combinePaths(directory, filename);

        s3Template.store(bucketName, directory, data);

        return new FileEntry(filename, URL_PREFIX + bucketName + "/" + directory);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data, boolean generateFilename)
        throws FileStorageException {

        return storeFileContent(directory, filename, data);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data) {
        return storeFileContent(directory, filename, data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data, boolean generateFilename)
        throws FileStorageException {

        return storeFileContent(directory, filename, data);
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

        return storeFileContent(directory, filename, inputStream);
    }

    private List<S3Resource> listAllObjects() {
        return s3Template.listObjects(bucketName, "");
    }

    private S3Resource getObject(String directoryPath, String filename) {
        List<S3Resource> s3Resources = s3Template.listObjects(bucketName, "");

        return s3Resources.stream()
            .filter((file) -> StringUtils.contains(file.getFilename(), directoryPath + "/" + filename))
            .findFirst()
            .orElseThrow(() -> new FileStorageException("File %s doesn't exist".formatted(filename)));
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
