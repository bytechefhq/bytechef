/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ftp.util;

import com.bytechef.component.exception.ProviderException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * FTP implementation of {@link RemoteFileClient} using Apache Commons Net.
 *
 * @author Ivica Cardic
 */
class FtpRemoteFileClient implements RemoteFileClient {

    private final FTPClient ftpClient;

    FtpRemoteFileClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }

    @Override
    public void storeFile(String remotePath, InputStream inputStream) throws IOException {
        boolean success = ftpClient.storeFile(remotePath, inputStream);

        if (!success) {
            throw new ProviderException("Failed to upload file: " + ftpClient.getReplyString());
        }
    }

    @Override
    public void retrieveFile(String remotePath, OutputStream outputStream) throws IOException {
        boolean success = ftpClient.retrieveFile(remotePath, outputStream);

        if (!success) {
            throw new ProviderException("Failed to download file: " + ftpClient.getReplyString());
        }
    }

    @Override
    public List<RemoteFileInfo> listFiles(String path) throws IOException {
        FTPFile[] files = ftpClient.listFiles(path);
        List<RemoteFileInfo> result = new ArrayList<>();

        for (FTPFile file : files) {
            String name = file.getName();

            if (name.equals(".") || name.equals("..")) {
                continue;
            }

            String filePath = path.endsWith("/") ? path + name : path + "/" + name;
            Calendar timestamp = file.getTimestamp();
            Instant modifiedAt = timestamp != null ? timestamp.toInstant() : null;

            result.add(new RemoteFileInfo(name, filePath, file.isDirectory(), file.getSize(), modifiedAt));
        }

        return result;
    }

    @Override
    public void deleteFile(String path) throws IOException {
        boolean success = ftpClient.deleteFile(path);

        if (!success) {
            throw new ProviderException("Failed to delete file: " + ftpClient.getReplyString());
        }
    }

    @Override
    public void deleteDirectory(String path) throws IOException {
        boolean success = ftpClient.removeDirectory(path);

        if (!success) {
            throw new ProviderException("Failed to delete directory: " + ftpClient.getReplyString());
        }
    }

    @Override
    public void rename(String oldPath, String newPath) throws IOException {
        boolean success = ftpClient.rename(oldPath, newPath);

        if (!success) {
            throw new ProviderException("Failed to rename/move: " + ftpClient.getReplyString());
        }
    }

    @Override
    public void createDirectoryTree(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            return;
        }

        String[] directories = path.split("/");
        StringBuilder currentPath = new StringBuilder();

        if (path.startsWith("/")) {
            currentPath.append("/");
        }

        for (String directory : directories) {
            if (directory.isEmpty()) {
                continue;
            }

            currentPath.append(directory)
                .append("/");

            String directoryPath = currentPath.toString();

            if (!ftpClient.changeWorkingDirectory(directoryPath)) {
                ftpClient.makeDirectory(directoryPath);
            }
        }

        ftpClient.changeWorkingDirectory("/");
    }

    @Override
    public boolean isDirectory(String path) throws IOException {
        FTPFile[] files = ftpClient.listFiles(path);

        if (files.length == 1 && files[0].isFile()) {
            return false;
        }

        return true;
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void close() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ioException) {
                // Intentionally ignored - best-effort cleanup during close
            }
        }
    }
}
