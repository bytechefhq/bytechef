/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.service;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsFileStorageService implements FileStorageService {

    @SuppressWarnings("PMD.UnusedPrivateField")
    private static final String URL_PREFIX = "aws://";

    @Override
    public void deleteFile(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        // TODO
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException {
        // TODO
        return false;
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull String nonRandomFilename)
        throws FileStorageException {
        // TODO
        return false;
    }

    @Override
    public FileEntry getFileEntry(@NonNull String directoryPath, @NonNull String nonRandomFilename)
        throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public Set<FileEntry> getFileEntries(@NonNull String directoryPath) throws FileStorageException {
        // TODO
        return Set.of();
    }

    @Override
    public Set<FileEntry> getFileEntries(@NonNull String directoryPath, String startWith) throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public InputStream getFileStream(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public URL getFileEntryURL(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        // TODO
        return null;
    }

    @Override
    public byte[] readFileToBytes(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {
        // TODO
        return new byte[0];
    }

    @Override
    public String readFileToString(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {
        // TODO
        return "";
    }

    @Override
    public FileEntry storeFileContent(@NonNull String directoryPath, @NonNull String filename, byte[] data)
        throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, byte[] data, boolean randomFilename)
        throws FileStorageException {

        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(@NonNull String directoryPath, @NonNull String filename, @NonNull String data)
        throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull String data, boolean randomFilename)
        throws FileStorageException {

        // TODO

        return null;
    }

    @Override
    public FileEntry
        storeFileContent(@NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream)
            throws FileStorageException {

        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream,
        boolean randomFilename) throws FileStorageException {

        // TODO

        return null;
    }
}
