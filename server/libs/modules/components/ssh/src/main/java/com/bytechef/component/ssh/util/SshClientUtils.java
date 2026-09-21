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

package com.bytechef.component.ssh.util;

import static com.bytechef.component.ssh.constant.SshConstants.HOST;
import static com.bytechef.component.ssh.constant.SshConstants.HOST_KEY_FINGERPRINT;
import static com.bytechef.component.ssh.constant.SshConstants.PASSPHRASE;
import static com.bytechef.component.ssh.constant.SshConstants.PASSWORD;
import static com.bytechef.component.ssh.constant.SshConstants.PORT;
import static com.bytechef.component.ssh.constant.SshConstants.PRIVATE_KEY;
import static com.bytechef.component.ssh.constant.SshConstants.USERNAME;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import java.io.IOException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.FingerprintVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.PasswordUtils;

/**
 * Creates SSH connections out of the connection parameters of the SSH component.
 *
 * @author Igor Beslic
 */
public class SshClientUtils {

    private static final int CONNECT_TIMEOUT_MILLIS = 30_000;
    private static final int DEFAULT_PORT = 22;

    private SshClientUtils() {
    }

    /**
     * Opens an SSH connection and authenticates it with either a password or a private key, depending on the connection
     * parameters. The caller owns the returned client and has to close it.
     */
    public static SSHClient connect(Parameters connectionParameters) {
        String privateKey = connectionParameters.getString(PRIVATE_KEY);

        SSHClient sshClient = new SSHClient();

        try {
            String hostKeyFingerprint = connectionParameters.getString(HOST_KEY_FINGERPRINT);

            if (hostKeyFingerprint == null || hostKeyFingerprint.isBlank()) {
                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            } else {
                sshClient.addHostKeyVerifier(FingerprintVerifier.getInstance(hostKeyFingerprint.trim()));
            }

            sshClient.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);

            String host = connectionParameters.getRequiredString(HOST);
            int port = connectionParameters.getInteger(PORT, DEFAULT_PORT);

            sshClient.connect(host, port);

            String username = connectionParameters.getRequiredString(USERNAME);

            if (privateKey == null || privateKey.isBlank()) {
                sshClient.authPassword(username, connectionParameters.getRequiredString(PASSWORD));
            } else {
                sshClient.authPublickey(username, loadKeyProvider(sshClient, privateKey, connectionParameters));
            }

            return sshClient;
        } catch (IOException ioException) {
            closeQuietly(sshClient);

            throw new ProviderException(
                "Failed to connect to the SSH server: " + ioException.getMessage(), ioException);
        } catch (RuntimeException runtimeException) {
            closeQuietly(sshClient);

            throw runtimeException;
        }
    }

    /**
     * Verifies that the connection parameters are usable by opening and immediately closing a connection.
     */
    public static void testConnection(Parameters connectionParameters) {
        SSHClient sshClient = connect(connectionParameters);

        closeQuietly(sshClient);
    }

    private static KeyProvider loadKeyProvider(
        SSHClient sshClient, String privateKey, Parameters connectionParameters) throws IOException {

        String passphrase = connectionParameters.getString(PASSPHRASE);

        PasswordFinder passwordFinder = passphrase == null || passphrase.isEmpty()
            ? null : PasswordUtils.createOneOff(passphrase.toCharArray());

        return sshClient.loadKeys(privateKey, null, passwordFinder);
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static void closeQuietly(SSHClient sshClient) {
        try {
            sshClient.close();
        } catch (IOException ioException) {
            // Intentionally ignored - best-effort cleanup during close
        }
    }
}
