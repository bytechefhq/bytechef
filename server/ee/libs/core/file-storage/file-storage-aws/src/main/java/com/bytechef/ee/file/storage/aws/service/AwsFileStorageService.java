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

    private static final String URL_PREFIX = "aws://";

    @Override
    public void deleteFile(String directoryPath, FileEntry fileEntry) {
        // TODO
    }

    @Override
    public boolean fileExists(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        // TODO
        return false;
    }

    @Override
    public boolean fileExists(String directoryPath, String nonRandomFilename) throws FileStorageException {
        // TODO
        return false;
    }

    @Override
    public FileEntry getFileEntry(String directoryPath, String nonRandomFilename) throws FileStorageException {
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
    public InputStream getFileStream(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public URL getFileEntryURL(String directoryPath, FileEntry fileEntry) {
        // TODO
        return null;
    }

    @Override
    public byte[] readFileToBytes(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        // TODO
        return new byte[0];
    }

    @Override
    public String readFileToString(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        // TODO
        return "";
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String filename, byte[] data) throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(
        String directoryPath, String filename, byte[] data, boolean randomFilename) throws FileStorageException {

        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String filename, String data) throws FileStorageException {
        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String filename, String data, boolean randomFilename)
        throws FileStorageException {

        // TODO

        return null;
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String filename, InputStream inputStream)
        throws FileStorageException {

        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(
        String directoryPath, String filename, InputStream inputStream, boolean randomFilename)
        throws FileStorageException {

        // TODO

        return null;
    }
}
