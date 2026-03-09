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

import static com.bytechef.component.ftp.constant.FtpConstants.HOST;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSIVE_MODE;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSWORD;
import static com.bytechef.component.ftp.constant.FtpConstants.PORT;
import static com.bytechef.component.ftp.constant.FtpConstants.SFTP;
import static com.bytechef.component.ftp.constant.FtpConstants.USERNAME;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Abstraction for remote file operations supporting both FTP and SFTP protocols.
 *
 * @author Ivica Cardic
 */
public interface RemoteFileClient extends Closeable {

    int DEFAULT_FTP_PORT = 21;
    int DEFAULT_SFTP_PORT = 22;

    static RemoteFileClient of(Parameters connectionParameters) {
        boolean useSftp = connectionParameters.getBoolean(SFTP, false);

        if (useSftp) {
            return createSftpClient(connectionParameters);
        }

        return createFtpClient(connectionParameters);
    }

    private static RemoteFileClient createFtpClient(Parameters connectionParameters) {
        FTPClient ftpClient = new FTPClient();

        String host = connectionParameters.getRequiredString(HOST);
        int port = connectionParameters.getInteger(PORT, DEFAULT_FTP_PORT);
        String username = connectionParameters.getRequiredString(USERNAME);
        String password = connectionParameters.getRequiredString(PASSWORD);
        boolean passiveMode = connectionParameters.getBoolean(PASSIVE_MODE, true);

        try {
            ftpClient.connect(host, port);

            int replyCode = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect();

                throw new ProviderException("FTP server refused connection: " + ftpClient.getReplyString());
            }

            boolean loginSuccess = ftpClient.login(username, password);

            if (!loginSuccess) {
                ftpClient.disconnect();

                throw new ProviderException("FTP login failed for user: " + username);
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            if (passiveMode) {
                ftpClient.enterLocalPassiveMode();
            } else {
                ftpClient.enterLocalActiveMode();
            }

            return new FtpRemoteFileClient(ftpClient);
        } catch (IOException ioException) {
            throw new ProviderException("Failed to connect to FTP server: " + ioException.getMessage(), ioException);
        }
    }

    private static RemoteFileClient createSftpClient(Parameters connectionParameters) {
        String host = connectionParameters.getRequiredString(HOST);
        int port = connectionParameters.getInteger(PORT, DEFAULT_SFTP_PORT);
        String username = connectionParameters.getRequiredString(USERNAME);
        String password = connectionParameters.getRequiredString(PASSWORD);

        try {
            SSHClient sshClient = new SSHClient();

            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(host, port);
            sshClient.authPassword(username, password);

            SFTPClient sftpClient = sshClient.newSFTPClient();

            return new SftpRemoteFileClient(sshClient, sftpClient);
        } catch (IOException ioException) {
            throw new ProviderException("Failed to connect to SFTP server: " + ioException.getMessage(), ioException);
        }
    }

    void storeFile(String remotePath, InputStream inputStream) throws IOException;

    void retrieveFile(String remotePath, OutputStream outputStream) throws IOException;

    List<RemoteFileInfo> listFiles(String path) throws IOException;

    void deleteFile(String path) throws IOException;

    void deleteDirectory(String path) throws IOException;

    void rename(String oldPath, String newPath) throws IOException;

    void createDirectoryTree(String path) throws IOException;

    boolean isDirectory(String path) throws IOException;

    record RemoteFileInfo(String name, String path, boolean directory, long size, Instant modifiedAt) {
    }
}
