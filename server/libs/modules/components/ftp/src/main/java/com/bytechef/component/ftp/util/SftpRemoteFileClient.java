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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;

/**
 * SFTP implementation of {@link RemoteFileClient} using sshj library.
 *
 * @author Ivica Cardic
 */
class SftpRemoteFileClient implements RemoteFileClient {

    private final SSHClient sshClient;
    private final SFTPClient sftpClient;

    SftpRemoteFileClient(SSHClient sshClient, SFTPClient sftpClient) {
        this.sshClient = sshClient;
        this.sftpClient = sftpClient;
    }

    @Override
    public void storeFile(String remotePath, InputStream inputStream) throws IOException {
        sftpClient.put(new InputStreamSourceFile(inputStream, remotePath), remotePath);
    }

    @Override
    public void retrieveFile(String remotePath, OutputStream outputStream) throws IOException {
        sftpClient.get(remotePath, new OutputStreamDestFile(outputStream));
    }

    @Override
    public List<RemoteFileInfo> listFiles(String path) throws IOException {
        List<RemoteResourceInfo> files = sftpClient.ls(path);
        List<RemoteFileInfo> result = new ArrayList<>();

        for (RemoteResourceInfo file : files) {
            String name = file.getName();

            if (name.equals(".") || name.equals("..")) {
                continue;
            }

            String filePath = path.endsWith("/") ? path + name : path + "/" + name;
            FileAttributes attributes = file.getAttributes();
            long modifiedTime = attributes.getMtime();
            Instant modifiedAt = modifiedTime > 0 ? Instant.ofEpochSecond(modifiedTime) : null;

            result.add(new RemoteFileInfo(name, filePath, file.isDirectory(), attributes.getSize(), modifiedAt));
        }

        return result;
    }

    @Override
    public void deleteFile(String path) throws IOException {
        sftpClient.rm(path);
    }

    @Override
    public void deleteDirectory(String path) throws IOException {
        sftpClient.rmdir(path);
    }

    @Override
    public void rename(String oldPath, String newPath) throws IOException {
        sftpClient.rename(oldPath, newPath);
    }

    @Override
    public void createDirectoryTree(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            return;
        }

        sftpClient.mkdirs(path);
    }

    @Override
    public boolean isDirectory(String path) throws IOException {
        FileAttributes attributes = sftpClient.stat(path);

        return attributes.getType() == FileMode.Type.DIRECTORY;
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void close() {
        if (sftpClient != null) {
            try {
                sftpClient.close();
            } catch (IOException ioException) {
                // Intentionally ignored - best-effort cleanup during close
            }
        }

        if (sshClient != null && sshClient.isConnected()) {
            try {
                sshClient.disconnect();
            } catch (IOException ioException) {
                // Intentionally ignored - best-effort cleanup during close
            }
        }
    }

    private static class InputStreamSourceFile extends InMemorySourceFile {

        private final InputStream inputStream;
        private final String name;

        InputStreamSourceFile(InputStream inputStream, String remotePath) {
            this.inputStream = inputStream;
            this.name = remotePath.substring(remotePath.lastIndexOf('/') + 1);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getLength() {
            return -1;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }
    }

    private static class OutputStreamDestFile extends InMemoryDestFile {

        private final OutputStream outputStream;

        OutputStreamDestFile(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public OutputStream getOutputStream(boolean append) {
            return outputStream;
        }

        @Override
        public long getLength() {
            return -1;
        }
    }
}
