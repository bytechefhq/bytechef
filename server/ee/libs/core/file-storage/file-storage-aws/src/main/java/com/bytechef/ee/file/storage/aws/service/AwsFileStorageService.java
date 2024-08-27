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

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsFileStorageService implements FileStorageService {

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
        // TODO
        return null;
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String filename, InputStream inputStream)
        throws FileStorageException {

        // TODO
        return null;
    }
}
